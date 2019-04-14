/*
 * Copyright 2019 Google LLC
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
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.rx.EnableState;
import com.google.android.gnd.rx.RxLocationServices;
import com.google.android.gnd.rx.RxLocationServices.RxFusedLocationProviderClient;
import com.google.android.gnd.rx.RxLocationServices.RxFusedLocationProviderClient.RxLocationCallback;
import com.google.android.gnd.vo.Point;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;

@ActivityScoped
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
  private final Subject<Location> locationUpdates;
  private final RxLocationCallback locationUpdateCallback;

  @Inject
  public LocationManager(
      Application app, PermissionsManager permissionsManager, SettingsManager settingsManager) {
    this.permissionsManager = permissionsManager;
    this.settingsManager = settingsManager;
    this.locationClient =
        RxLocationServices.getFusedLocationProviderClient(app.getApplicationContext());
    this.locationUpdates = BehaviorSubject.create();
    this.locationUpdateCallback = RxLocationCallback.create(locationUpdates);
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
    // There sometimes noticeable latency between when location update request succeeds and when
    // the first location update is received. Requesting the last know location is usually
    // immediate, so we merge into the stream to reduce perceived latency.
    return getLastLocation()
        .toObservable()
        .mergeWith(locationUpdates.map(LocationManager::toPoint))
        .toFlowable(BackpressureStrategy.LATEST);
  }

  /**
   * Asynchronously try to enable location permissions and settings, and if successful, turns on
   * location updates exposed by {@link #getLocationUpdates()}.
   */
  public synchronized Single<EnableState> enableLocationUpdates() {
    Log.d(TAG, "Attempting to enable location updates");
    return permissionsManager
        .obtainPermission(ACCESS_FINE_LOCATION)
        .andThen(settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST))
        .andThen(
            locationClient.requestLocationUpdates(
                FINE_LOCATION_UPDATES_REQUEST, locationUpdateCallback))
        .toSingle(() -> EnableState.enabled())
        .onErrorReturn(t -> EnableState.error(t));
  }

  // TODO: Request/remove updates on resume/pause.
  public synchronized Single<EnableState> disableLocationUpdates() {
    // Ignore errors when removing location updates, usually caused by disabling the same callback
    // multiple times.
    return locationClient
        .removeLocationUpdates(locationUpdateCallback)
        .toSingle(() -> EnableState.disabled())
        .doOnError(t -> Log.v(TAG, "disableLocationUpdates:", t))
        .onErrorReturn(__ -> EnableState.disabled());
  }

  @SuppressLint("MissingPermission")
  private Maybe<Point> getLastLocation() {
    return locationClient.getLastLocation().map(LocationManager::toPoint);
  }
}
