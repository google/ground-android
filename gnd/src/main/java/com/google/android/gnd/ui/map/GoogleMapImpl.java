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

package com.google.android.gnd.ui.map;

import static com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION;

import android.annotation.SuppressLint;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gnd.model.PlaceIcon;
import com.google.android.gnd.model.Point;
import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import java.util.HashMap;
import java.util.Map;

// TODO: Refactor into generic Map interface.
public class GoogleMapImpl {

  private GoogleMap map;
  private boolean enabled;
  private Map<String, Marker> markers = new HashMap<>();
  private LatLng cameraTargetBeforeUserPan;
  private ReplaySubject<MapMarker> markerClickSubject = ReplaySubject.create();
  private ReplaySubject<Point> userPanSubject = ReplaySubject.create();

  public GoogleMapImpl(GoogleMap map) {
    this.map = map;
    map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    map.getUiSettings().setRotateGesturesEnabled(false);
    map.getUiSettings().setMyLocationButtonEnabled(false);
    map.getUiSettings().setMapToolbarEnabled(false);
    map.setOnCameraIdleListener(this::onCameraIdle);
    map.setOnCameraMoveStartedListener(this::onCameraMoveStarted);
    map.setOnCameraMoveListener(this::onCameraMove);
    map.setOnMarkerClickListener(
        marker -> {
          if (enabled) {
            markerClickSubject.onNext((MapMarker) marker.getTag());
            // Allow map to pan to marker.
            return false;
          } else {
            // Prevent map from panning to marker.
            return true;
          }
        });
    this.enabled = true;
  }

  public Observable<MapMarker> markerClicks() {
    return markerClickSubject;
  }

  public Observable<Point> userPans() {
    return userPanSubject;
  }

  public void enable() {
    enabled = true;
    map.getUiSettings().setAllGesturesEnabled(true);
  }

  public void disable() {
    enabled = false;
    map.getUiSettings().setAllGesturesEnabled(false);
  }

  public void moveCamera(Point point) {
    map.moveCamera(CameraUpdateFactory.newLatLng(toLatLng(point)));
  }

  public void moveCamera(Point point, float zoomLevel) {
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(toLatLng(point), zoomLevel));
  }

  public void addOrUpdateMarker(
      MapMarker mapMarker, boolean hasPendingWrites, boolean isHighlighted) {
    Marker marker = markers.get(mapMarker.getId());
    LatLng position = toLatLng(mapMarker.getPosition());
    PlaceIcon icon = mapMarker.getIcon();
    BitmapDescriptor bitmap =
        isHighlighted
            ? icon.getWhiteBitmap()
            : (hasPendingWrites ? icon.getGreyBitmap() : icon.getBitmap());
    if (marker == null) {
      marker = map.addMarker(new MarkerOptions().position(position).icon(bitmap).alpha(1.0f));
      markers.put(mapMarker.getId(), marker);
    } else {
      marker.setIcon(bitmap);
      marker.setPosition(position);
    }
    marker.setTag(mapMarker);
  }

  public void removeMarker(String id) {
    Marker marker = markers.get(id);
    if (marker == null) {
      return;
    }
    marker.remove();
    markers.remove(id);
  }

  public void removeAllMarkers() {
    map.clear();
    markers.clear();
  }

  public Point getCenter() {
    return toPoint(map.getCameraPosition().target);
  }

  public float getCurrentZoomLevel() {
    return map.getCameraPosition().zoom;
  }

  @SuppressLint("MissingPermission")
  public void enableCurrentLocationIndicator() {
    map.setMyLocationEnabled(true);
  }


  private void onCameraIdle() {
    cameraTargetBeforeUserPan = null;
  }

  private void onCameraMoveStarted(int reason) {
    if (reason == REASON_DEVELOPER_ANIMATION) {
      // Map was panned by the app, not the user.
      return;
    }
    cameraTargetBeforeUserPan = map.getCameraPosition().target;
  }

  private void onCameraMove() {
    if (cameraTargetBeforeUserPan == null) {
      return;
    }
    LatLng cameraTarget = map.getCameraPosition().target;
    if (!cameraTarget.equals(cameraTargetBeforeUserPan)) {
      userPanSubject.onNext(toPoint(cameraTarget));
    }
  }

  private static Point toPoint(LatLng latLng) {
    return Point.newBuilder().setLatitude(latLng.latitude).setLongitude(latLng.longitude).build();
  }

  private static LatLng toLatLng(Point position) {
    return new LatLng(position.getLatitude(), position.getLongitude());
  }
}
