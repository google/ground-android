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
import com.google.android.gnd.model.Feature;
import com.google.android.gnd.model.GndDataRepository;
import com.google.android.gnd.model.Point;
import com.google.android.gnd.system.LocationManager;
import com.google.android.gnd.system.LocationManager.LocationFailureReason;
import com.google.android.gnd.ui.map.GoogleMapsView;
import com.google.android.gnd.ui.map.MapMarker;

public class MapPresenter {
  private static final float DEFAULT_ZOOM_LEVEL = 14.0f;

  private final MainPresenter mainPresenter;
  private final MainActivity mainActivity;
  private final GndDataRepository model;
  private final LocationManager locationManager;
  private GoogleMapsView mapView;
  private FloatingActionButton addBtn;
  private FloatingActionButton locationLockBtn;
  private boolean locationLockEnabled;
  private boolean zoomOnNextLocationUpdate;

  public MapPresenter(
      MainPresenter mainPresenter,
      MainActivity mainActivity,
      GndDataRepository model,
      LocationManager locationManager) {
    this.mainPresenter = mainPresenter;
    this.mainActivity = mainActivity;
    this.model = model;
    this.locationManager = locationManager;
  }

  void onCreate(Bundle savedInstanceState) {
    mapView = mainActivity.getMapView();
    mapView.onCreate(savedInstanceState);
    mapView.initialize(this::onMapReady, this::onMarkerClick, this::onCameraMove);

    locationLockBtn = mainActivity.getLocationLockButton();
    locationLockBtn.setOnClickListener((v) -> this.onLocationLockClick());
    locationLockBtn.bringToFront();

    addBtn = mainActivity.getAddFeatureButton();
    addBtn.setOnClickListener((v) -> mainPresenter.onAddFeatureClick());
    addBtn.bringToFront();
  }

  private void onMapReady(GoogleMapsView map) {
    mainPresenter.onMapReady();
    enableLocationLock();
  }

  void onStart() {
    if (locationLockEnabled) {
      enableLocationLock();
    }
  }

  void onResume() {
    mapView.onResume();
  }

  void onPause() {
    mapView.onPause();
  }

  void onStop() {
    mapView.onStop();
    locationManager.removeLocationUpdates();
  }

  void onDestroy() {
    mapView.onDestroy();
  }

  void onLowMemory() {
    mapView.onLowMemory();
  }

  private void onMarkerClick(MapMarker marker) {
    if (marker.getObject() instanceof Feature) {
      mainPresenter.showFeatureDetails((Feature) marker.getObject());
    }
  }

  private void onCameraMove() {
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
    mainPresenter
        .getPermissionManager()
        .obtainFineLocationPermission(
            () ->
                locationManager.checkLocationSettings(
                    this::requestLocationUpdates, this::onLocationFailure),
            this::onFineLocationPermissionsDenied);
  }

  private void onFineLocationPermissionsDenied() {
    mainActivity.showUserActionFailureMessage(R.string.no_fine_location_permissions);
  }

  public void onLocationLockSettingsIssueResolved() {
    requestLocationUpdates();
  }

  private void requestLocationUpdates() {
    locationManager.requestLocationUpdates(
        this::onRequestLocationUpdatesSuccess, this::onLocationFailure, this::onLocationUpdate);
  }

  private void onRequestLocationUpdatesSuccess() {
    mapView.enableCurrentLocationIndicator();
    zoomOnNextLocationUpdate = true;
    locationManager.requestLastLocation(this::onLocationUpdate);
    locationLockEnabled = true;
    locationLockBtn.setImageResource(R.drawable.ic_gps_blue);
  }

  public void onLocationFailure(LocationFailureReason reason) {
    disableLocationLock();
    switch (reason) {
      case UNEXPECTED_ERROR:
        mainActivity.showUserActionFailureMessage(R.string.location_updates_unknown_error);
        break;
      case SETTINGS_CHANGE_UNAVAILABLE:
        mainActivity.showUserActionFailureMessage(R.string.location_disabled_in_settings);
        break;
      case SETTINGS_CHANGE_FAILED:
        mainActivity.showUserActionFailureMessage(R.string.location_disabled_in_settings);
        break;
      case LOCATION_UPDATES_REQUEST_FAILED:
        mainActivity.showUserActionFailureMessage(R.string.location_updates_request_failed);
        break;
    }
  }

  private void onLocationUpdate(Point location) {
    if (zoomOnNextLocationUpdate) {
      mapView.moveCamera(location, Math.max(DEFAULT_ZOOM_LEVEL, mapView.getCurrentZoomLevel()));
      zoomOnNextLocationUpdate = false;
    } else {
      mapView.moveCamera(location);
    }
  }

  private void disableLocationLock() {
    locationManager.removeLocationUpdates();
    locationLockEnabled = false;
    locationLockBtn.setImageResource(R.drawable.ic_gps_grey600);
  }
}
