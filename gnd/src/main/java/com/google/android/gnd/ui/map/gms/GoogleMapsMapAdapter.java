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
import static java8.util.stream.StreamSupport.stream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.FeatureType;
import com.google.android.gnd.model.layer.Style;
import com.google.android.gnd.ui.MapIcon;
import com.google.android.gnd.ui.map.MapMarker;
import com.google.android.gnd.ui.map.MapProvider.MapAdapter;
import com.google.common.collect.ImmutableSet;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java8.util.Optional;
import javax.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Wrapper around {@link GoogleMap}, exposing Google Maps API functionality to Ground as a {@link
 * MapAdapter}.
 */
class GoogleMapsMapAdapter implements MapAdapter {

  private static final String TAG = GoogleMapsMapAdapter.class.getSimpleName();
  private static final String GEO_JSON_FILE = "gnd-geojson.geojson";
  private final GoogleMap map;
  private final Context context;
  /**
   * Cache of ids to map markers. We don't mind this being destroyed on lifecycle events since the
   * GoogleMap markers themselves are destroyed as well.
   */
  private java.util.Map<String, Marker> markers = new HashMap<>();

  private final PublishSubject<MapMarker> markerClickSubject = PublishSubject.create();
  private final PublishSubject<Point> dragInteractionSubject = PublishSubject.create();
  private final BehaviorSubject<Point> cameraPositionSubject = BehaviorSubject.create();

  @Nullable private LatLng cameraTargetBeforeDrag;

  public GoogleMapsMapAdapter(GoogleMap map, Context context) {
    this.map = map;
    this.context = context;
    map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    UiSettings uiSettings = map.getUiSettings();
    uiSettings.setRotateGesturesEnabled(false);
    uiSettings.setTiltGesturesEnabled(false);
    uiSettings.setMyLocationButtonEnabled(false);
    uiSettings.setMapToolbarEnabled(false);
    uiSettings.setCompassEnabled(false);
    uiSettings.setIndoorLevelPickerEnabled(false);
    map.setOnMarkerClickListener(this::onMarkerClick);
    map.setOnCameraIdleListener(this::onCameraIdle);
    map.setOnCameraMoveStartedListener(this::onCameraMoveStarted);
    map.setOnCameraMoveListener(this::onCameraMove);
    onCameraMove();
  }

  public void renderJsonLayer() {
    File file = new File(context.getFilesDir(), GEO_JSON_FILE);

    try {
      InputStream is = new FileInputStream(file);
      BufferedReader buf = new BufferedReader(new InputStreamReader(is));
      String line = buf.readLine();
      StringBuilder sb = new StringBuilder();
      while (line != null) {
        sb.append(line).append('\n');
        line = buf.readLine();
      }

      JSONObject geoJson = new JSONObject(sb.toString());
      GeoJsonLayer layer = new GeoJsonLayer(map, geoJson);
      layer.addLayerToMap();
      Log.d(TAG, "JSON layer successfully loaded");

    } catch (IOException | JSONException e) {
      Log.e(TAG, "Unable to load JSON layer", e);
    }
  }

  private boolean onMarkerClick(Marker marker) {
    if (map.getUiSettings().isZoomGesturesEnabled()) {
      markerClickSubject.onNext((MapMarker) marker.getTag());
      // Allow map to pan to marker.
      return false;
    } else {
      // Prevent map from panning to marker.
      return true;
    }
  }

  @Override
  public Observable<MapMarker> getMarkerClicks() {
    return markerClickSubject;
  }

  @Override
  public Observable<Point> getDragInteractions() {
    return dragInteractionSubject;
  }

  @Override
  public Observable<Point> getCameraPosition() {
    return cameraPositionSubject;
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
  public void moveCamera(Point point) {
    map.moveCamera(CameraUpdateFactory.newLatLng(point.toLatLng()));
  }

  @Override
  public void moveCamera(Point point, float zoomLevel) {
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(point.toLatLng(), zoomLevel));
  }

  private void addMarker(MapMarker mapMarker, boolean hasPendingWrites, boolean isHighlighted) {
    LatLng position = mapMarker.getPosition().toLatLng();
    MapIcon icon = mapMarker.getIcon();
    BitmapDescriptor bitmap =
        isHighlighted
            ? icon.getWhiteBitmap()
            : hasPendingWrites ? icon.getGreyBitmap() : icon.getBitmap();
    Marker marker = map.addMarker(new MarkerOptions().position(position).icon(bitmap).alpha(1.0f));
    markers.put(mapMarker.getId(), marker);
    marker.setTag(mapMarker);
  }

  private void removeAllMarkers() {
    for (Marker marker : markers.values()) {
      marker.remove();
    }
    markers.clear();
  }

  @Override
  public Point getCenter() {
    return Point.fromLatLng(map.getCameraPosition().target);
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
  public void updateMarkers(ImmutableSet<Feature> features) {
    if (features.isEmpty()) {
      removeAllMarkers();
      return;
    }
    Set<Feature> newFeatures = new HashSet<>(features);
    Iterator<Entry<String, Marker>> it = markers.entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, Marker> entry = it.next();
      Marker marker = entry.getValue();
      getMapMarker(marker)
          .flatMap(MapMarker::getFeature)
          .ifPresent(
              feature -> {
                if (features.contains(feature)) {
                  newFeatures.remove(feature);
                } else {
                  removeMarker(marker);
                  it.remove();
                }
              });
    }
    stream(newFeatures).forEach(this::addMarker);
  }

  private Optional<MapMarker> getMapMarker(Marker marker) {
    Object tag = marker.getTag();
    return tag != null && tag instanceof MapMarker
        ? Optional.of((MapMarker) tag)
        : Optional.empty();
  }

  private void removeMarker(Marker marker) {
    Log.v(TAG, "Removing marker " + marker.getId());
    marker.remove();
  }

  private void addMarker(Feature feature) {
    Log.v(TAG, "Adding marker for " + feature.getId());
    FeatureType featureType = feature.getFeatureType();
    Style style = featureType.getDefaultStyle();
    String color = style == null ? null : style.getColor();
    String overlayId = null; // Not yet implemented.
    MapIcon icon = new MapIcon(context, overlayId, color);
    // TODO: Reimplement hasPendingWrites.
    addMarker(
        MapMarker.newBuilder()
            .setId(feature.getId())
            .setPosition(feature.getPoint())
            .setIcon(icon)
            .setObject(feature)
            .build(),
        false,
        false);
  }

  private void onCameraIdle() {
    cameraTargetBeforeDrag = null;
  }

  private void onCameraMoveStarted(int reason) {
    if (reason == REASON_DEVELOPER_ANIMATION) {
      // MapAdapter was panned by the app, not the user.
      return;
    }
    cameraTargetBeforeDrag = map.getCameraPosition().target;
  }

  private void onCameraMove() {
    LatLng cameraTarget = map.getCameraPosition().target;
    Point target = Point.fromLatLng(cameraTarget);
    cameraPositionSubject.onNext(target);
    if (cameraTargetBeforeDrag != null && !cameraTarget.equals(cameraTargetBeforeDrag)) {
      dragInteractionSubject.onNext(target);
    }
  }
}
