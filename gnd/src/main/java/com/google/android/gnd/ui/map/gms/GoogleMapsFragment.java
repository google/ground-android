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
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
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
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.map.CameraPosition;
import com.google.android.gnd.ui.map.MapFeature;
import com.google.android.gnd.ui.map.MapFragment;
import com.google.android.gnd.ui.map.MapGeoJson;
import com.google.android.gnd.ui.map.MapPin;
import com.google.android.gnd.ui.map.MapPolygon;
import com.google.android.gnd.ui.map.MapType;
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
import dagger.hilt.android.AndroidEntryPoint;
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
import java8.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * Customization of Google Maps API Fragment that automatically adjusts the Google watermark based
 * on window insets.
 */
@AndroidEntryPoint
public class GoogleMapsFragment extends SupportMapFragment implements MapFragment {

  // TODO(#936): Remove placeholder with appropriate images
  private static final ImmutableList<MapType> MAP_TYPES =
      ImmutableList.<MapType>builder()
          .add(new MapType(GoogleMap.MAP_TYPE_NORMAL, R.string.road_map, R.drawable.ground_logo))
          .add(new MapType(GoogleMap.MAP_TYPE_TERRAIN, R.string.terrain, R.drawable.ground_logo))
          .add(new MapType(GoogleMap.MAP_TYPE_HYBRID, R.string.satellite, R.drawable.ground_logo))
          .build();

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
   * References to Google Maps SDK Markers present on the map. Used to sync and update polylines
   * with current view and data state.
   */
  private final Set<Marker> markers = new HashSet<>();

  private final Map<MapFeature, GeometryCollection> geoJsonGeometries = new HashMap<>();
  private final Map<MapFeature, Polyline> polygons = new HashMap<>();
  @Inject BitmapUtil bitmapUtil;
  @Inject MarkerIconFactory markerIconFactory;
  @Nullable private GoogleMap map;

  /**
   * References to Google Maps SDK CustomCap present on the map. Used to set the custom drawable to
   * start and end of polygon.
   */
  private CustomCap customCap;

  private int cameraChangeReason = REASON_DEVELOPER_ANIMATION;

  private static Point fromLatLng(LatLng latLng) {
    return Point.newBuilder().setLatitude(latLng.latitude).setLongitude(latLng.longitude).build();
  }

  private static LatLng toLatLng(Point point) {
    return new LatLng(point.getLatitude(), point.getLongitude());
  }

  private static boolean containsLocation(LatLng latLng, Polygon polygon) {
    if (stream(polygon.getHoles())
        .anyMatch(hole -> PolyUtil.containsLocation(latLng, hole, polygon.isGeodesic()))) {
      return false;
    }

    return PolyUtil.containsLocation(latLng, polygon.getPoints(), polygon.isGeodesic());
  }

  private WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
    int insetBottom = insets.getSystemWindowInsetBottom();
    // TODO: Move extra padding to dimens.xml.
    // HACK: Fix padding when keyboard is shown; we limit the padding here to prevent the
    // watermark from flying up too high due to the combination of translateY and big inset
    // size due to keyboard.
    setWatermarkPadding(view, 20, 0, 0, Math.min(insetBottom, 250) + 8);
    return insets;
  }

  private void setWatermarkPadding(View view, int left, int top, int right, int bottom) {
    ImageView watermark = view.findViewWithTag("GoogleWatermark");
    // Watermark may be null if Maps failed to load.
    if (watermark == null) {
      return;
    }
    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) watermark.getLayoutParams();
    params.setMargins(left, top, right, bottom);
    watermark.setLayoutParams(params);
  }

  @Override
  public ImmutableList<MapType> getAvailableMapTypes() {
    return MAP_TYPES;
  }

  @NonNull
  private GoogleMap getMap() {
    if (map == null) {
      throw new IllegalStateException("Map is not ready");
    }
    return map;
  }

  @Override
  public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    customCap = new CustomCap(bitmapUtil.bitmapDescriptorFromVector(R.drawable.ic_endpoint));
  }

  @NonNull
  @Override
  public View onCreateView(
      @NonNull LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
    View view = super.onCreateView(layoutInflater, viewGroup, bundle);
    ViewCompat.setOnApplyWindowInsetsListener(view, this::onApplyWindowInsets);
    return view;
  }

  @Override
  public void attachToFragment(
      @NonNull AbstractFragment containerFragment,
      @IdRes int containerId,
      @NonNull Consumer<MapFragment> mapReadyAction) {
    containerFragment.replaceFragment(containerId, this);
    getMapAsync(
        googleMap -> {
          onMapReady(googleMap);
          mapReadyAction.accept(this);
        });
  }

  private void onMapReady(GoogleMap map) {
    this.map = map;

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

  // Handle taps on ambiguous features.
  private void handleAmbiguity(LatLng latLng) {
    Builder<MapFeature> candidates = ImmutableList.builder();
    ArrayList<String> processed = new ArrayList<>();

    for (Entry<MapFeature, GeometryCollection> geoJsonEntry : geoJsonGeometries.entrySet()) {
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

  private boolean onMarkerClick(Marker marker) {
    if (getMap().getUiSettings().isZoomGesturesEnabled()) {
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
  public double getDistanceInPixels(Point point1, Point point2) {
    if (map == null) {
      Timber.e("Null Map reference");
      return 0;
    }
    Projection projection = map.getProjection();
    android.graphics.Point loc1 = projection.toScreenLocation(toLatLng(point1));
    android.graphics.Point loc2 = projection.toScreenLocation(toLatLng(point2));
    double dx = loc1.x - loc2.x;
    double dy = loc1.y - loc2.y;
    return Math.sqrt(dx * dx + dy * dy);
  }

  @Override
  public void enableGestures() {
    getMap().getUiSettings().setAllGesturesEnabled(true);
  }

  @Override
  public void disableGestures() {
    getMap().getUiSettings().setAllGesturesEnabled(false);
  }

  @Override
  public void moveCamera(Point point) {
    getMap().moveCamera(CameraUpdateFactory.newLatLng(toLatLng(point)));
  }

  @Override
  public void moveCamera(Point point, float zoomLevel) {
    getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(toLatLng(point), zoomLevel));
  }

  private void addMapPin(MapPin mapPin) {
    LatLng position = toLatLng(mapPin.getPosition());
    // TODO: add the anchor values into the resource dimensions file
    Marker marker =
        getMap()
            .addMarker(
                new MarkerOptions()
                    .position(position)
                    .icon(getMarkerIcon(mapPin))
                    .anchor(0.5f, 0.85f)
                    .alpha(1.0f));
    markers.add(marker);
    marker.setTag(mapPin);
  }

  private BitmapDescriptor getMarkerIcon(MapPin mapPin) {
    String color = mapPin.getStyle().getColor();
    return markerIconFactory.getMarkerIcon(parseColor(color), getCurrentZoomLevel());
  }

  private void addMapPolyline(MapPolygon mapPolygon) {
    PolylineOptions options = new PolylineOptions();
    options.clickable(false);
    ImmutableList<LatLng> vertices =
        stream(mapPolygon.getVertices())
            .map(GoogleMapsFragment::toLatLng)
            .collect(toImmutableList());
    options.addAll(vertices);

    Polyline polyline = getMap().addPolyline(options);
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
    return (int) getResources().getDimension(R.dimen.polyline_stroke_width);
  }

  private void onMapClick(LatLng latLng) {
    handleAmbiguity(latLng);
  }

  @Override
  public float getCurrentZoomLevel() {
    return getMap().getCameraPosition().zoom;
  }

  @Override
  @SuppressLint("MissingPermission")
  public void enableCurrentLocationIndicator() {
    if (!getMap().isMyLocationEnabled()) {
      getMap().setMyLocationEnabled(true);
    }
  }

  @Override
  public void setMapFeatures(ImmutableSet<MapFeature> features) {
    Timber.v("setMapFeatures() called with %s features", features.size());
    Set<MapFeature> featuresToUpdate = new HashSet<>(features);

    List<Marker> deletedMarkers = new ArrayList<>();
    for (Marker marker : markers) {
      MapPin pin = (MapPin) marker.getTag();
      if (features.contains(pin)) {
        // If existing pin is present and up-to-date, don't update it.
        featuresToUpdate.remove(pin);
      } else {
        // If pin isn't present or up-to-date, remove it so it can be added back later.
        removeMarker(marker);
        deletedMarkers.add(marker);
      }
    }

    // Update markers list.
    stream(deletedMarkers).forEach(markers::remove);

    Iterator<Entry<MapFeature, Polyline>> polylineIterator = polygons.entrySet().iterator();
    while (polylineIterator.hasNext()) {
      Entry<MapFeature, Polyline> entry = polylineIterator.next();
      MapFeature mapFeature = entry.getKey();
      Polyline polyline = entry.getValue();
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

    if (!featuresToUpdate.isEmpty()) {
      Timber.v("Updating %d features", featuresToUpdate.size());
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
  }

  @Override
  public void refreshMarkerIcons() {
    for (Marker marker : markers) {
      MapPin mapPin = (MapPin) marker.getTag();

      if (mapPin != null) {
        marker.setIcon(getMarkerIcon(mapPin));
      }
    }
  }

  @Override
  public int getMapType() {
    return getMap().getMapType();
  }

  @Override
  public void setMapType(int mapType) {
    getMap().setMapType(mapType);
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
      Timber.w("Invalid color code in job style: %s", colorHexCode);
      return getResources().getColor(R.color.colorMapAccent);
    }
  }

  private void onCameraIdle() {
    if (cameraChangeReason == REASON_GESTURE) {
      LatLng target = getMap().getCameraPosition().target;
      float zoom = getMap().getCameraPosition().zoom;
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
    return getMap().getProjection().getVisibleRegion().latLngBounds;
  }

  @Override
  public void setViewport(LatLngBounds bounds) {
    getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
  }

  private void addTileOverlay(String filePath) {
    File mbtilesFile = new File(requireContext().getFilesDir(), filePath);

    if (!mbtilesFile.exists()) {
      Timber.i("mbtiles file %s does not exist", mbtilesFile.getAbsolutePath());
      return;
    }

    try {
      MapBoxOfflineTileProvider tileProvider = new MapBoxOfflineTileProvider(mbtilesFile);
      tileProviders.onNext(tileProvider);
      getMap().addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
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
    getMap().addTileOverlay(new TileOverlayOptions().tileProvider(webTileProvider));
  }

  @Override
  public void addRemoteTileOverlays(ImmutableList<String> urls) {
    stream(urls).forEach(this::addRemoteTileOverlay);
  }

  private void addMapGeoJson(MapGeoJson mapFeature) {
    GeoJsonParser geoJsonParser = new GeoJsonParser(mapFeature.getGeoJson());
    GeometryCollection featureGeometries = new GeometryCollection();
    geoJsonGeometries.put(mapFeature, featureGeometries);
    for (GeoJsonFeature geoJsonFeature : geoJsonParser.getFeatures()) {
      featureGeometries.addGeometry(mapFeature, geoJsonFeature.getGeometry());
    }
  }

  /** A collection of geometries in a GeoJson feature. */
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
      return getMap().addMarker(new MarkerOptions().zIndex(1).position(point.getCoordinates()));
    }

    private Polyline addPolyline(GeoJsonLineString lineString, float width, int color) {
      return getMap()
          .addPolyline(
              new PolylineOptions()
                  .width(width)
                  .color(color)
                  .zIndex(1)
                  .addAll(lineString.getCoordinates()));
    }

    private Polygon addPolygon(GeoJsonPolygon dataPolygon, float width, int color) {
      PolygonOptions polygonOptions =
          new PolygonOptions()
              .addAll(dataPolygon.getOuterBoundaryCoordinates())
              .strokeWidth(width)
              .strokeColor(color)
              .clickable(false)
              .zIndex(1);
      for (List<LatLng> innerBoundary : dataPolygon.getInnerBoundaryCoordinates()) {
        polygonOptions.addHole(innerBoundary);
      }

      return getMap().addPolygon(polygonOptions);
    }
  }
}
