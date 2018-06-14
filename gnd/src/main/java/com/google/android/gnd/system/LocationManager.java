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

package com.google.android.gnd.system;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.annotation.SuppressLint;
import android.app.Application;
import android.location.Location;
import android.util.Log;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gnd.inject.PerActivity;
import com.google.android.gnd.rx.RxLocationServices;
import com.google.android.gnd.rx.RxLocationServices.RxFusedLocationProviderClient;
import com.google.android.gnd.vo.Point;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import javax.inject.Inject;

@PerActivity
public class LocationManager {
  private static final String TAG = LocationManager.class.getSimpleName();
  private static final long UPDATE_INTERVAL = 1000 /* 1 sec */;
  private static final long FASTEST_INTERVAL = 250;
  private static final LocationRequest FINE_LOCATION_UPDATES_REQUEST =
      new LocationRequest()
          .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
          .setInterval(UPDATE_INTERVAL)
          .setFastestInterval(FASTEST_INTERVAL);
  private final PermissionsManager permissionsManager;
  private final SettingsManager settingsManager;
  private final RxFusedLocationProviderClient locationClient;

  @Inject
  public LocationManager(
      Application app, PermissionsManager permissionsManager, SettingsManager settingsManager) {
    this.permissionsManager = permissionsManager;
    this.settingsManager = settingsManager;
    this.locationClient =
      RxLocationServices.getFusedLocationProviderClient(app.getApplicationContext());
  }

  private static Point toPoint(Location location) {
    return Point.newBuilder()
        .setLatitude(location.getLatitude())
        .setLongitude(location.getLongitude())
        .build();
  }

  /**
   * Returns the location update stream. New subscribers and downstream subscribers that can't keep
   * up will only see the latest location.
   */
  public Flowable<Point> getLocationUpdates() {
    return locationClient.getLocationUpdates().map(LocationManager::toPoint);
  }

  /**
   * Asynchronously try to enable location permissions and settings, and if successful, turns on
   * location updates exposed by {@link #getLocationUpdates()}.
   */
  public Completable enableLocationUpdates() {
    Log.d(TAG, "Attempting to enable location updates");
    return permissionsManager
      .obtainPermission(ACCESS_FINE_LOCATION)
      .andThen(settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST))
      .andThen(locationClient.requestLocationUpdates(FINE_LOCATION_UPDATES_REQUEST));
  }

  // TODO: Request/remove updates on resume/pause.
  public Completable disableLocationUpdates() {
    return locationClient.removeLocationUpdates();
  }

  @SuppressLint("MissingPermission")
  public Single<Point> getLastLocation() {
    // TODO: Should we be sending the request onSubscribe instead of immediately? In this specific
    // case it might not matter, but there may be others where it does?
    Log.d(TAG, "Requesting last known location");
    return locationClient.getLastLocation().map(LocationManager::toPoint);
  }
}
