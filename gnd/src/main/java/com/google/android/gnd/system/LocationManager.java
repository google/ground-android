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
import android.location.Location;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.rx.BooleanOrError;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.rx.RxFusedLocationProviderClient;
import com.google.android.gnd.system.rx.RxLocationCallback;
import dagger.hilt.android.scopes.ActivityScoped;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;
import timber.log.Timber;

@ActivityScoped
public class LocationManager {
  private static final long UPDATE_INTERVAL = 1000 /* 1 sec */;
  private static final long FASTEST_INTERVAL = 250; /* 250 ms */
  private static final LocationRequest FINE_LOCATION_UPDATES_REQUEST =
      new LocationRequest()
          .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
          .setInterval(UPDATE_INTERVAL)
          .setFastestInterval(FASTEST_INTERVAL);

  private final PermissionsManager permissionsManager;
  private final SettingsManager settingsManager;
  private final RxFusedLocationProviderClient locationClient;
  @Hot(replays = true)
  private final Subject<Location> locationUpdates = BehaviorSubject.create();
  private final RxLocationCallback locationUpdateCallback;

  @Inject
  public LocationManager(
      PermissionsManager permissionsManager,
      SettingsManager settingsManager,
      RxFusedLocationProviderClient locationClient) {
    this.permissionsManager = permissionsManager;
    this.settingsManager = settingsManager;
    this.locationClient = locationClient;
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
  public synchronized Single<BooleanOrError> enableLocationUpdates() {
    Timber.d("Attempting to enable location updates");
    return permissionsManager
        .obtainPermission(ACCESS_FINE_LOCATION)
        .andThen(settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST))
        .andThen(
            locationClient.requestLocationUpdates(
                FINE_LOCATION_UPDATES_REQUEST, locationUpdateCallback))
        .toSingle(BooleanOrError::trueValue)
        .onErrorReturn(BooleanOrError::error);
  }

  // TODO: Request/remove updates on resume/pause.
  public synchronized Single<BooleanOrError> disableLocationUpdates() {
    // Ignore errors when removing location updates, usually caused by disabling the same callback
    // multiple times.
    return locationClient
        .removeLocationUpdates(locationUpdateCallback)
        .toSingle(BooleanOrError::falseValue)
        .doOnError(t -> Timber.e(t, "disableLocationUpdates"))
        .onErrorReturn(__ -> BooleanOrError.falseValue());
  }

  @SuppressLint("MissingPermission")
  private Maybe<Point> getLastLocation() {
    return locationClient.getLastLocation().map(LocationManager::toPoint);
  }
}
