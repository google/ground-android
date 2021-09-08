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
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;
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
   * Manager for handling click events for markers.
   *
   * <p>This isn't needed if the map only has a single layer. But in our case, multiple GeoJSON
   * layers might be added and we wish to have independent clickable features for each layer.
   */
  private final MarkerManager markerManager;
  // TODO: Add managers for polyline layers

  /**
   * References to Google Maps SDK Markers present on the map. Used to sync and update markers with
   * current view and data state.
   */
  private final MarkerManager.Collection markers;

  /**
   * References to Google Maps SDK CustomCap present on the map. Used to set the custom drawable to
   * start and end of polygon.
   */
  private final CustomCap customCap;

  /**
   * References to Google Maps SDK Markers present on the map. Used to sync and update polylines
   * with current view and data state.
   */
  private final Set<Polyline> polylines = new HashSet<>();

  private final Map<MapFeature, List<LatLng>> geoJsonPolygonLoops = new HashMap<>();
  private final Map<MapFeature, ArrayList<ArrayList<LatLng>>> geoJsonPolygonHoles = new HashMap<>();
  private final Map<MapFeature, List<LatLng>> polygons = new HashMap<>();
  /**
   * References to Google Maps SDK GeoJSON layers present on the map, keyed by MapGeoJson features.
   * Used to sync and update GeoJSON with current data and UI state.
   */
  private Map<MapGeoJson, GeoJsonLayer> geoJsonLayersByFeature = new HashMap<>();

  private int cameraChangeReason = REASON_DEVELOPER_ANIMATION;

  public GoogleMapsMapAdapter(
      GoogleMap map, Context context, MarkerIconFactory markerIconFactory, BitmapUtil bitmapUtil) {
    this.map = map;
    this.context = context;
    this.markerIconFactory = markerIconFactory;
    this.customCap = new CustomCap(bitmapUtil.bitmapDescriptorFromVector(R.drawable.ic_endpoint));

    // init markers
    markerManager = new MarkerManager(map);
    markers = markerManager.newCollection();
    markers.setOnMarkerClickListener(this::onMarkerClick);

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

    for (Map.Entry<MapFeature, ArrayList<ArrayList<LatLng>>> json :
        geoJsonPolygonHoles.entrySet()) {
      ArrayList<ArrayList<LatLng>> holes = json.getValue();
      if (processed.contains(((MapGeoJson) json.getKey()).getId())) {
        continue;
      }

      if (stream(holes).anyMatch(hole -> PolyUtil.containsLocation(latLng, hole, false))) {
        processed.add(((MapGeoJson) json.getKey()).getId());
      }
    }

    for (Map.Entry<MapFeature, List<LatLng>> json : geoJsonPolygonLoops.entrySet()) {
      if (processed.contains(((MapGeoJson) json.getKey()).getId())) {
        continue;
      }

      if (PolyUtil.containsLocation(latLng, json.getValue(), false)) {
        candidates.add(json.getKey());
        processed.add(((MapGeoJson) json.getKey()).getId());
      }
    }

    for (Map.Entry<MapFeature, List<LatLng>> entry : polygons.entrySet()) {
      List<LatLng> vertices = entry.getValue();
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
        markers.addMarker(new MarkerOptions().position(position).icon(icon).alpha(1.0f));
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

    polylines.add(polyline);
    polygons.put(mapPolygon, vertices);
  }

  private boolean isPolygonCompleted(List<Point> vertices) {
    return vertices.size() > 2 && vertices.get(vertices.size() - 1) == vertices.get(0);
  }

  private int getPolylineStrokeWidth() {
    return (int) context.getResources().getDimension(R.dimen.polyline_stroke_width);
  }

  private void addMapGeoJson(MapGeoJson mapFeature) {
    // Pass markerManager here otherwise markers in the previous layers won't be clickable.
    GeoJsonLayer layer =
        new GeoJsonLayer(map, mapFeature.getGeoJson(), markerManager, null, null, null);

    int width = mapFeature.getStrokeWidth();
    int color = parseColor(mapFeature.getStyle().getColor());

    GeoJsonPointStyle pointStyle = layer.getDefaultPointStyle();
    pointStyle.setZIndex(1);

    GeoJsonPolygonStyle polygonStyle = layer.getDefaultPolygonStyle();
    polygonStyle.setStrokeWidth(width);
    polygonStyle.setLineStringWidth(width);
    polygonStyle.setStrokeColor(color);
    polygonStyle.setClickable(false);
    polygonStyle.setZIndex(1);

    GeoJsonLineStringStyle lineStringStyle = layer.getDefaultLineStringStyle();
    lineStringStyle.setLineStringWidth(width);
    lineStringStyle.setZIndex(1);

    layer.addLayerToMap();

    for (GeoJsonFeature geoJsonFeature : layer.getFeatures()) {
      updateGeoJsonPolygonBoundaries(geoJsonFeature, mapFeature);
    }

    geoJsonLayersByFeature.put(mapFeature, layer);
  }

  /* Adds the inner and outer boundaries (holes and loops) of polygons defined by a GeoJson feature
  to the adapters lists of known polygon boundaries, associating them with the given MapFeature. */
  private void updateGeoJsonPolygonBoundaries(
      GeoJsonFeature geoJsonFeature, MapFeature mapFeature) {
    if ("Polygon".equals(geoJsonFeature.getGeometry().getGeometryType())) {
      GeoJsonPolygon polygon = (GeoJsonPolygon) geoJsonFeature.getGeometry();

      geoJsonPolygonLoops.put(mapFeature, polygon.getOuterBoundaryCoordinates());
      geoJsonPolygonHoles.put(mapFeature, polygon.getInnerBoundaryCoordinates());
    }
    if ("MultiPolygon".equals(geoJsonFeature.getGeometry().getGeometryType())) {
      GeoJsonMultiPolygon multi = (GeoJsonMultiPolygon) geoJsonFeature.getGeometry();

      for (GeoJsonPolygon polygon : multi.getPolygons()) {
        geoJsonPolygonLoops.put(mapFeature, polygon.getOuterBoundaryCoordinates());
        geoJsonPolygonHoles.put(mapFeature, polygon.getInnerBoundaryCoordinates());
      }
    }
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

    for (Marker marker : markers.getMarkers()) {
      MapPin pin = (MapPin) marker.getTag();
      if (features.contains(pin)) {
        // If existing pin is present and up-to-date, don't update it.
        featuresToUpdate.remove(pin);
      } else {
        // If pin isn't present or up-to-date, remove it so it can be added back later.
        removeMarker(marker);
      }
    }

    Iterator<Polyline> polylineIterator = polylines.iterator();
    while (polylineIterator.hasNext()) {
      Polyline polyline = polylineIterator.next();
      MapPolygon polygon = (MapPolygon) polyline.getTag();
      if (features.contains(polygon)) {
        // If polygon already exists on map, don't add it.
        featuresToUpdate.remove(polygon);
      } else {
        // Remove existing polyline not in list of updatedFeatures.
        removePolygon(polyline);
        polylineIterator.remove();
      }
    }

    // Iterate over all existing GeoJSON on the map.
    Iterator<Entry<MapGeoJson, GeoJsonLayer>> geoJsonIterator =
        geoJsonLayersByFeature.entrySet().iterator();
    while (geoJsonIterator.hasNext()) {
      Entry<MapGeoJson, GeoJsonLayer> entry = geoJsonIterator.next();
      MapGeoJson geoJsonFeature = entry.getKey();
      GeoJsonLayer layer = entry.getValue();
      if (features.contains(geoJsonFeature)) {
        // If existing GeoJSON is present and up-to-date, don't update it.
        featuresToUpdate.remove(geoJsonFeature);
      } else {
        // If pin isn't present or up-to-date, remove it so it can be added back later.
        Timber.v("Removing GeoJSON feature %s", geoJsonFeature.getFeature().getId());
        geoJsonPolygonHoles.remove(geoJsonFeature);
        geoJsonPolygonLoops.remove(geoJsonFeature);
        geoJsonIterator.remove();
        layer.removeLayerFromMap();
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
}
