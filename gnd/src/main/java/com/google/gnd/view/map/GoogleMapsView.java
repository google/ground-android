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

package com.google.gnd.view.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gnd.model.PlaceIcon;
import com.google.gnd.model.Point;

import java.util.HashMap;
import java.util.Map;

import java8.util.function.Consumer;

import static com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
    .REASON_DEVELOPER_ANIMATION;

// TODO: Refactor view interface and adapter and allow plugging different map providers.
public class GoogleMapsView extends MapView {
  private static final String TAG = GoogleMapsView.class.getSimpleName();
  private GoogleMap map;
  private Map<String, Marker> markers;
  private boolean enabled;
  private LatLng cameraTargetBeforeUserPan;

  public GoogleMapsView(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    markers = new HashMap<>();
  }

  private static Point toPoint(LatLng latLng) {
    return Point.newBuilder().setLatitude(latLng.latitude).setLongitude(latLng.longitude).build();
  }

  private static LatLng toLatLng(Point position) {
    return new LatLng(position.getLatitude(), position.getLongitude());
  }

  public void initialize(
      Consumer<GoogleMapsView> onReady, Consumer<MapMarker> onMarkerClick, Runnable onMapPan) {
    GoogleMapsView view = this;
    getMapAsync(
        googleMap -> {
          map = googleMap;
          map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
          map.getUiSettings().setRotateGesturesEnabled(false);
          map.getUiSettings().setMyLocationButtonEnabled(false);
          map.getUiSettings().setMapToolbarEnabled(false);
          map.setOnMarkerClickListener(
              marker -> {
                if (enabled) {
                  onMarkerClick.accept((MapMarker) marker.getTag());
                  return false; // Allow map to pan to marker.
                } else {
                  return true; // Prevent map from panning to marker.
                }
              });
          map.setOnCameraIdleListener(this::onCameraIdle);
          map.setOnCameraMoveStartedListener(this::onCameraMoveStarted);
          map.setOnCameraMoveListener(() -> onCameraMove(onMapPan));
          enabled = true;
          onReady.accept(view);
        });
  }

  private void onCameraIdle() {
    cameraTargetBeforeUserPan = null;
  }

  private void onCameraMoveStarted(int reason) {
    if (reason == REASON_DEVELOPER_ANIMATION) {
      // Map was panned by app to track current location.
      return;
    }
    cameraTargetBeforeUserPan = map.getCameraPosition().target;
  }

  private void onCameraMove(Runnable onMapPan) {
    if (cameraTargetBeforeUserPan == null) {
      return;
    }
    LatLng cameraTarget = map.getCameraPosition().target;
    if (!cameraTarget.equals(cameraTargetBeforeUserPan)) {
      onMapPan.run();
    }
  }

  @SuppressLint("MissingPermission")
  public void enableCurrentLocationIndicator() {
    map.setMyLocationEnabled(true);
  }

  public void moveCamera(Point point) {
    map.moveCamera(CameraUpdateFactory.newLatLng(toLatLng(point)));
  }

  public void moveCamera(Point point, float zoomLevel) {
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(toLatLng(point), zoomLevel));
  }

  public void setOtherMarkersAlpha(float a, String featureId) {
    for (Map.Entry<String, Marker> entry : markers.entrySet()) {
      if (!entry.getKey().equals(featureId)) {
        entry.getValue().setAlpha(a);
      }
    }
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

  public void enable() {
    enabled = true;
    map.getUiSettings().setAllGesturesEnabled(true);
  }

  public void disable() {
    enabled = false;
    map.getUiSettings().setAllGesturesEnabled(false);
  }

  public Point getCenter() {
    return toPoint(map.getCameraPosition().target);
  }

  public float getCurrentZoomLevel() {
    return map.getCameraPosition().zoom;
  }

  private void setWatermarkPadding(int left, int top, int right, int bottom) {
    ImageView watermark = findViewWithTag("GoogleWatermark");
    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) watermark.getLayoutParams();
    params.setMargins(left, top, right, bottom);
    watermark.setLayoutParams(params);
  }

  @Override
  public WindowInsets onApplyWindowInsets(WindowInsets insets) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
      int insetBottom = insets.getSystemWindowInsetBottom();
      // TODO: Move extra padding to dimens.xml.
      // HACK: Fix padding when keyboard is shown; we limit the padding here to prevent the
      // watermark from flying up too high due to the combination of translateY and big inset
      // size due to keyboard.
      setWatermarkPadding(20, 0, 0, Math.min(insetBottom, 250) + 8);
    }
    return super.onApplyWindowInsets(insets);
  }
}
