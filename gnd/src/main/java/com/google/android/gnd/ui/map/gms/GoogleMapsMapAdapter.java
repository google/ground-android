/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gnd.ui.map.gms;

import static com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION;
import static com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE;
import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gnd.R;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.MarkerIconFactory;
import com.google.android.gnd.ui.map.CameraPosition;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapFeature;
import com.google.android.gnd.ui.map.MapGeoJson;
import com.google.android.gnd.ui.map.MapPin;
import com.google.android.gnd.ui.map.MapPolygon;
import com.google.android.gnd.ui.util.BitmapUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.data.Geometry;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.data.geojson.GeoJsonLineString;
import com.google.maps.android.data.geojson.GeoJsonMultiLineString;
import com.google.maps.android.data.geojson.GeoJsonMultiPoint;
import com.google.maps.android.data.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.data.geojson.GeoJsonParser;
import com.google.maps.android.data.geojson.GeoJsonPoint;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import timber.log.Timber;

/**
 * Wrapper around {@link GoogleMap}, exposing Google Maps SDK functionality to Ground as a {@link
 * MapAdapter}.
 */
class GoogleMapsMapAdapter implements MapAdapter {

  private final GoogleMap map;
  private final Context context;
  private final MarkerIconFactory markerIconFactory;

  /** Marker click events. */
  @Hot private final Subject<MapPin> markerClicks = PublishSubject.create();

  /** Ambiguous click events. */
  @Hot private final Subject<ImmutableList<MapFeature>> featureClicks = PublishSubject.create();

  /** Map drag events. Emits items when the map drag has started. */
  @Hot private final FlowableProcessor<Nil> startDragEvents = PublishProcessor.create();

  /** Camera move events. Emits items after the camera has stopped moving. */
  @Hot
  private final FlowableProcessor<CameraPosition> cameraMovedEvents = PublishProcessor.create();

  // TODO(#693): Simplify impl of tile providers.
  // TODO(#691): This is a limitation of the MapBox tile provider we're using;
  // since one need to call `close` explicitly, we cannot generically expose these as TileProviders;
  // instead we must retain explicit reference to the concrete type.
  @Hot
  private final PublishSubject<MapBoxOfflineTileProvider> tileProviders = PublishSubject.create();

  /**
   * References to Google Maps SDK CustomCap present on the map. Used to set the custom drawable to
   * start and end of polygon.
   */
  private final CustomCap customCap;

  /**
   * References to Google Maps SDK Markers present on the map. Used to sync and update polylines
   * with current view and data state.
   */
  private final Set<Marker> markers = new HashSet<>();

  private final Map<MapFeature, GeometryCollection> geoJsonGeometries = new HashMap<>();
  private final Map<MapFeature, Polyline> polygons = new HashMap<>();

  private int cameraChangeReason = REASON_DEVELOPER_ANIMATION;

  public GoogleMapsMapAdapter(
      GoogleMap map, Context context, MarkerIconFactory markerIconFactory, BitmapUtil bitmapUtil) {
    this.map = map;
    this.context = context;
    this.markerIconFactory = markerIconFactory;
    this.customCap = new CustomCap(bitmapUtil.bitmapDescriptorFromVector(R.drawable.ic_endpoint));

    map.setOnMarkerClickListener(this::onMarkerClick);

    UiSettings uiSettings = map.getUiSettings();
    uiSettings.setRotateGesturesEnabled(false);
    uiSettings.setTiltGesturesEnabled(false);
    uiSettings.setMyLocationButtonEnabled(false);
    uiSettings.setMapToolbarEnabled(false);
    uiSettings.setCompassEnabled(false);
    uiSettings.setIndoorLevelPickerEnabled(false);
    map.setOnCameraIdleListener(this::onCameraIdle);
    map.setOnCameraMoveStartedListener(this::onCameraMoveStarted);
    map.setOnMapClickListener(this::onMapClick);
  }

  private static Point fromLatLng(LatLng latLng) {
    return Point.newBuilder().setLatitude(latLng.latitude).setLongitude(latLng.longitude).build();
  }

  private static LatLng toLatLng(Point point) {
    return new LatLng(point.getLatitude(), point.getLongitude());
  }

  // Handle taps on ambiguous features.
  private void handleAmbiguity(LatLng latLng) {
    Builder<MapFeature> candidates = ImmutableList.builder();
    ArrayList<String> processed = new ArrayList<>();

    for (Entry<MapFeature, GeometryCollection> geoJsonEntry :
        geoJsonGeometries.entrySet()) {
      MapGeoJson geoJsonFeature = (MapGeoJson) geoJsonEntry.getKey();
      GeometryCollection geoJsonGeometry = geoJsonEntry.getValue();
      if (processed.contains(geoJsonFeature.getId())) {
        continue;
      }

      if (stream(geoJsonGeometry.polygons)
          .anyMatch((polygon) -> containsLocation(latLng, polygon))) {
        candidates.add(geoJsonFeature);
      }

      processed.add(geoJsonFeature.getId());
    }

    for (Entry<MapFeature, Polyline> entry : polygons.entrySet()) {
      List<LatLng> vertices = entry.getValue().getPoints();
      MapFeature mapFeature = entry.getKey();
      if (processed.contains(((MapPolygon) mapFeature).getId())) {
        continue;
      }

      if (PolyUtil.containsLocation(latLng, vertices, false)) {
        candidates.add(mapFeature);
        processed.add(((MapPolygon) mapFeature).getId());
      }
    }
    ImmutableList<MapFeature> result = candidates.build();
    if (!result.isEmpty()) {
      featureClicks.onNext(result);
    }
  }

  private static boolean containsLocation(LatLng latLng, Polygon polygon) {
    if (stream(polygon.getHoles())
        .anyMatch(hole -> PolyUtil.containsLocation(latLng, hole, polygon.isGeodesic()))) {
      return false;
    }

    return PolyUtil.containsLocation(latLng, polygon.getPoints(), polygon.isGeodesic());
  }

  private boolean onMarkerClick(Marker marker) {
    if (map.getUiSettings().isZoomGesturesEnabled()) {
      markerClicks.onNext((MapPin) marker.getTag());
      // Allow map to pan to marker.
      return false;
    } else {
      // Prevent map from panning to marker.
      return true;
    }
  }

  @Hot
  @Override
  public Observable<MapPin> getMapPinClicks() {
    return markerClicks;
  }

  @Override
  public @Hot Observable<ImmutableList<MapFeature>> getFeatureClicks() {
    return featureClicks;
  }

  @Hot
  @Override
  public Flowable<Nil> getStartDragEvents() {
    return startDragEvents;
  }

  @Hot
  @Override
  public Flowable<CameraPosition> getCameraMovedEvents() {
    return cameraMovedEvents;
  }

  @Hot
  @Override
  public Observable<MapBoxOfflineTileProvider> getTileProviders() {
    return tileProviders;
  }

  @Override
  public void enable() {
    map.getUiSettings().setAllGesturesEnabled(true);
  }

  @Override
  public void disable() {
    map.getUiSettings().setAllGesturesEnabled(false);
  }

  @Override
  public void moveCamera(CameraPosition position) {
    map.moveCamera(
        CameraUpdateFactory.newLatLngZoom(toLatLng(position.getTarget()), position.getZoomLevel()));
  }

  @Override
  public void moveCamera(Point point) {
    map.moveCamera(CameraUpdateFactory.newLatLng(toLatLng(point)));
  }

  @Override
  public void moveCamera(Point point, float zoomLevel) {
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(toLatLng(point), zoomLevel));
  }

  private void addMapPin(MapPin mapPin) {
    LatLng position = toLatLng(mapPin.getPosition());
    String color = mapPin.getStyle().getColor();
    BitmapDescriptor icon = markerIconFactory.getMarkerIcon(parseColor(color));
    Marker marker =
        map.addMarker(new MarkerOptions().position(position).icon(icon).alpha(1.0f));
    markers.add(marker);
    marker.setTag(mapPin);
  }

  private void addMapPolyline(MapPolygon mapPolygon) {
    PolylineOptions options = new PolylineOptions();
    options.clickable(false);
    ImmutableList<LatLng> vertices =
        stream(mapPolygon.getVertices())
            .map(GoogleMapsMapAdapter::toLatLng)
            .collect(toImmutableList());
    options.addAll(vertices);

    Polyline polyline = map.addPolyline(options);
    polyline.setTag(mapPolygon);
    if (!isPolygonCompleted(mapPolygon.getVertices())) {
      polyline.setStartCap(customCap);
      polyline.setEndCap(customCap);
    }
    polyline.setWidth(getPolylineStrokeWidth());
    polyline.setColor(parseColor(mapPolygon.getStyle().getColor()));
    polyline.setJointType(JointType.ROUND);

    polygons.put(mapPolygon, polyline);
  }

  private boolean isPolygonCompleted(List<Point> vertices) {
    return vertices.size() > 2 && vertices.get(vertices.size() - 1) == vertices.get(0);
  }

  private int getPolylineStrokeWidth() {
    return (int) context.getResources().getDimension(R.dimen.polyline_stroke_width);
  }

  private void onMapClick(LatLng latLng) {
    handleAmbiguity(latLng);
  }

  @Override
  public Point getCameraTarget() {
    return fromLatLng(map.getCameraPosition().target);
  }

  @Override
  public float getCurrentZoomLevel() {
    return map.getCameraPosition().zoom;
  }

  @Override
  @SuppressLint("MissingPermission")
  public void enableCurrentLocationIndicator() {
    if (!map.isMyLocationEnabled()) {
      map.setMyLocationEnabled(true);
    }
  }

  @Override
  public void setMapFeatures(ImmutableSet<MapFeature> features) {
    Timber.d("Set map features called : %s", features.size());
    Set<MapFeature> featuresToUpdate = new HashSet<>(features);

    for (Marker marker : markers) {
      MapPin pin = (MapPin) marker.getTag();
      if (features.contains(pin)) {
        // If existing pin is present and up-to-date, don't update it.
        featuresToUpdate.remove(pin);
      } else {
        // If pin isn't present or up-to-date, remove it so it can be added back later.
        removeMarker(marker);
      }
    }

    Iterator<Entry<MapFeature, Polyline>> polylineIterator = polygons.entrySet().iterator();
    while (polylineIterator.hasNext()) {
      MapFeature mapFeature = polylineIterator.next().getKey();
      Polyline polyline = polylineIterator.next().getValue();
      if (features.contains(mapFeature)) {
        // If polygon already exists on map, don't add it.
        featuresToUpdate.remove(mapFeature);
      } else {
        // Remove existing polyline not in list of updatedFeatures.
        removePolygon(polyline);
        polylineIterator.remove();
      }
    }

    // Iterate over all existing GeoJSON on the map.
    Iterator<Entry<MapFeature, GeometryCollection>> geoJsonIterator =
        geoJsonGeometries.entrySet().iterator();
    while (geoJsonIterator.hasNext()) {
      Entry<MapFeature, GeometryCollection> entry = geoJsonIterator.next();
      MapFeature mapFeature = entry.getKey();
      GeometryCollection featureGeometries = entry.getValue();
      if (features.contains(mapFeature)) {
        // If existing GeoJSON is present and up-to-date, don't update it.
        featuresToUpdate.remove(mapFeature);
      } else {
        // If GeoJSON isn't present or up-to-date, remove it so it can be added back later.
        Timber.v(
            "Removing GeoJSON feature %s", Objects.requireNonNull(mapFeature.getFeature()).getId());
        geoJsonIterator.remove();
        featureGeometries.remove();
      }
    }

    for (MapFeature mapFeature : featuresToUpdate) {
      if (mapFeature instanceof MapPin) {
        addMapPin((MapPin) mapFeature);
      } else if (mapFeature instanceof MapPolygon) {
        addMapPolyline((MapPolygon) mapFeature);
      } else if (mapFeature instanceof MapGeoJson) {
        addMapGeoJson((MapGeoJson) mapFeature);
      }
    }
  }

  @Override
  public int getMapType() {
    return map.getMapType();
  }

  @Override
  public void setMapType(int mapType) {
    map.setMapType(mapType);
  }

  private void removeMarker(Marker marker) {
    Timber.v("Removing marker %s", marker.getId());
    marker.remove();
  }

  private void removePolygon(Polyline polyline) {
    Timber.v("Removing polyline %s", polyline.getId());
    polyline.remove();
  }

  private int parseColor(@Nullable String colorHexCode) {
    try {
      return Color.parseColor(String.valueOf(colorHexCode));
    } catch (IllegalArgumentException e) {
      Timber.w("Invalid color code in layer style: %s", colorHexCode);
      return context.getResources().getColor(R.color.colorMapAccent);
    }
  }

  private void onCameraIdle() {
    if (cameraChangeReason == REASON_GESTURE) {
      LatLng target = map.getCameraPosition().target;
      float zoom = map.getCameraPosition().zoom;
      cameraMovedEvents.onNext(new CameraPosition(fromLatLng(target), zoom));
      cameraChangeReason = REASON_DEVELOPER_ANIMATION;
    }
  }

  private void onCameraMoveStarted(int reason) {
    cameraChangeReason = reason;
    if (reason == REASON_GESTURE) {
      startDragEvents.onNext(Nil.NIL);
    }
  }

  @Override
  public LatLngBounds getViewport() {
    return map.getProjection().getVisibleRegion().latLngBounds;
  }

  @Override
  public void setBounds(LatLngBounds bounds) {
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
  }

  private void addTileOverlay(String filePath) {
    File mbtilesFile = new File(context.getFilesDir(), filePath);

    if (!mbtilesFile.exists()) {
      Timber.i("mbtiles file %s does not exist", mbtilesFile.getAbsolutePath());
      return;
    }

    try {
      MapBoxOfflineTileProvider tileProvider = new MapBoxOfflineTileProvider(mbtilesFile);
      tileProviders.onNext(tileProvider);
      map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
    } catch (Exception e) {
      Timber.e(e, "Couldn't initialize tile provider for mbtiles file %s", mbtilesFile);
    }
  }

  @Override
  public void addLocalTileOverlays(ImmutableSet<String> mbtilesFiles) {
    stream(mbtilesFiles).forEach(this::addTileOverlay);
  }

  private void addRemoteTileOverlay(String url) {
    WebTileProvider webTileProvider = new WebTileProvider(url);
    map.addTileOverlay(new TileOverlayOptions().tileProvider(webTileProvider));
  }

  @Override
  public void addRemoteTileOverlays(ImmutableList<String> urls) {
    stream(urls).forEach(this::addRemoteTileOverlay);
  }

  /**
   * A collection of geometries in a GeoJson feature.
   */
  private class GeometryCollection {
    List<Marker> markers = new ArrayList<>();
    List<Polyline> polylines = new ArrayList<>();
    List<Polygon> polygons = new ArrayList<>();

    void remove() {
      for (Marker marker : markers) {
        marker.remove();
      }

      for (Polyline polyline : polylines) {
        polyline.remove();
      }

      for (Polygon polygon : polygons) {
        polygon.remove();
      }
    }

    void addGeometry(MapGeoJson mapFeature, Geometry<?> geometry) {
      int width = mapFeature.getStrokeWidth();
      int color = parseColor(mapFeature.getStyle().getColor());
      switch (geometry.getGeometryType()) {
        case "Point":
          markers.add(addMarker((GeoJsonPoint) geometry));
          break;
        case "LineString":
          polylines.add(addPolyline((GeoJsonLineString) geometry, width, color));
          break;
        case "Polygon":
          polygons.add(addPolygon((GeoJsonPolygon) geometry, width, color));
          break;
        case "MultiPoint":
          List<GeoJsonPoint> points = ((GeoJsonMultiPoint) geometry).getPoints();
          for (GeoJsonPoint point : points) {
            markers.add(addMarker(point));
          }
          break;
        case "MultiLineString":
          for (GeoJsonLineString lineString :
              ((GeoJsonMultiLineString) geometry).getLineStrings()) {
            addPolyline(lineString, width, color);
          }
          break;
        case "MultiPolygon":
          for (GeoJsonPolygon polygon : ((GeoJsonMultiPolygon) geometry).getPolygons()) {
            polygons.add(addPolygon(polygon, width, color));
          }
          break;
        case "GeometryCollection":
          for (Geometry<?> singleGeometry :
              ((GeoJsonGeometryCollection) geometry).getGeometries()) {
            addGeometry(mapFeature, singleGeometry);
          }
          break;
        default:
          Timber.w("Unsupported geometry type %s", geometry.getGeometryType());
          break;
      }
    }

    private Marker addMarker(GeoJsonPoint point) {
      return map.addMarker(
          new MarkerOptions()
              .zIndex(1)
              .position(point.getCoordinates()));
    }

    private Polyline addPolyline(GeoJsonLineString lineString, float width, int color) {
      return map.addPolyline(
          new PolylineOptions()
              .width(width)
              .color(color)
              .zIndex(1)
              .addAll(lineString.getCoordinates()));
    }

    private Polygon addPolygon(GeoJsonPolygon dataPolygon, float width, int color) {
      PolygonOptions polygonOptions = new PolygonOptions()
          .addAll(dataPolygon.getOuterBoundaryCoordinates())
          .strokeWidth(width)
          .strokeColor(color)
          .clickable(false)
          .zIndex(1);
      for (List<LatLng> innerBoundary : dataPolygon.getInnerBoundaryCoordinates()) {
        polygonOptions.addHole(innerBoundary);
      }

      return map.addPolygon(polygonOptions);
    }
  }

  private void addMapGeoJson(MapGeoJson mapFeature) {
    GeoJsonParser geoJsonParser = new GeoJsonParser(mapFeature.getGeoJson());
    GeometryCollection featureGeometries = new GeometryCollection();
    geoJsonGeometries.put(mapFeature, featureGeometries);
    for (GeoJsonFeature geoJsonFeature: geoJsonParser.getFeatures()) {
      featureGeometries.addGeometry(mapFeature, geoJsonFeature.getGeometry());
    }
  }
}
