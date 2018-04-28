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

package com.google.android.gnd;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import com.google.android.gnd.model.Place;
import com.google.android.gnd.model.Point;
import com.google.android.gnd.system.LocationManager;
import com.google.android.gnd.system.PermissionsManager.PermissionDeniedException;
import com.google.android.gnd.system.SettingsManager.SettingsChangeRequestCanceled;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapMarker;

public class MapPresenter {

  private static final String TAG = MapPresenter.class.getSimpleName();
  private static final float DEFAULT_ZOOM_LEVEL = 14.0f;

  private final MainPresenter mainPresenter;
  private final MainActivity mainActivity;
  private final LocationManager locationManager;
  private MapAdapter mapAdapter;
  private FloatingActionButton addBtn;
  private FloatingActionButton locationLockBtn;
  private boolean locationLockEnabled;
  private boolean zoomOnNextLocationUpdate;

  public MapPresenter(
      MainPresenter mainPresenter,
      MainActivity mainActivity,
      LocationManager locationManager) {
    this.mainPresenter = mainPresenter;
    this.mainActivity = mainActivity;
    this.locationManager = locationManager;
  }

  void onCreate(Bundle savedInstanceState) {
    mapAdapter = mainActivity.getMapAdapter();
    mapAdapter.map().subscribe(map -> {
      map.markerClicks().subscribe(this::onMarkerClick);
      map.userPans().subscribe(this::onCameraMove);
      enableLocationLock();
    });

    locationLockBtn = mainActivity.getLocationLockButton();
    locationLockBtn.setOnClickListener((v) -> this.onLocationLockClick());
    locationLockBtn.bringToFront();

    addBtn = mainActivity.getAddPlaceButton();
    addBtn.setOnClickListener((v) -> mainPresenter.onAddPlaceClick());
    addBtn.bringToFront();
  }

  void onStart() {
    if (locationLockEnabled) {
      enableLocationLock();
    }
  }

  void onStop() {
    locationManager.removeLocationUpdates();
  }

  private void onMarkerClick(MapMarker marker) {
    if (marker.getObject() instanceof Place) {
      mainPresenter.showPlaceDetails((Place) marker.getObject());
    }
  }

  private void onCameraMove(Point newLocation) {
    if (locationLockEnabled) {
      disableLocationLock();
    }
  }

  public void onLocationLockClick() {
    if (locationLockEnabled) {
      disableLocationLock();
    } else {
      enableLocationLock();
    }
  }

  private void enableLocationLock() {
    Log.d(TAG, "Enabling location lock");
    locationManager.enableLocationUpdates()
        .subscribe(this::onLocationUpdate, this::onLocationFailure);
  }

  private void onLocationFailure(Throwable t) {
    // TODO: Turn user-visible errors into a stream the activity can subscribe to.
    if (t instanceof PermissionDeniedException) {
      mainActivity.showUserActionFailureMessage(R.string.no_fine_location_permissions);
    } else if (t instanceof SettingsChangeRequestCanceled) {
      mainActivity.showUserActionFailureMessage(R.string.location_disabled_in_settings);
    } else {
      mainActivity.showUserActionFailureMessage(R.string.location_updates_unknown_error);
    }
  }

  private void onRequestLocationUpdatesSuccess() {
    if (!locationLockEnabled) {
      Log.d(TAG, "Location lock enabled");
      mapAdapter.map().subscribe(MapAdapter.Map::enableCurrentLocationIndicator);
      zoomOnNextLocationUpdate = true;
      locationLockEnabled = true;
      locationLockBtn.setImageResource(R.drawable.ic_gps_blue);
    }
  }

  private void onLocationUpdate(Point location) {
    mapAdapter.map().subscribe(map -> {
      onRequestLocationUpdatesSuccess();
      if (zoomOnNextLocationUpdate) {
        map.moveCamera(location, Math.max(DEFAULT_ZOOM_LEVEL, map.getCurrentZoomLevel()));
        zoomOnNextLocationUpdate = false;
      } else {
        map.moveCamera(location);
      }
    });
  }

  private void disableLocationLock() {
    locationManager.removeLocationUpdates();
    locationLockEnabled = false;
    locationLockBtn.setImageResource(R.drawable.ic_gps_grey600);
  }
}
