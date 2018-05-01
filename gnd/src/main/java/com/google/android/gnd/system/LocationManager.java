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

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gnd.inject.PerActivity;
import com.google.android.gnd.model.Point;
import com.google.android.gnd.rx.RxTask;

import javax.inject.Inject;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.subjects.PublishSubject;

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
  private final Context context;
  private final PermissionsManager permissionsManager;
  private final SettingsManager settingsManager;
  private LocationCallbackImpl locationCallback;
  private PublishSubject<Point> locationUpdateSubject;

  @Inject
  public LocationManager(
      Application app, PermissionsManager permissionsManager, SettingsManager settingsManager) {
    this.context = app.getApplicationContext();
    this.permissionsManager = permissionsManager;
    this.settingsManager = settingsManager;
    this.locationUpdateSubject = PublishSubject.create();
  }

  private Completable enableLocationSettings() {
    return settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST);
  }

  private static Point toPoint(Location location) {
    return Point.newBuilder()
        .setLatitude(location.getLatitude())
        .setLongitude(location.getLongitude())
        .build();
  }

  public Flowable<Point> locationUpdates() {
    return locationUpdateSubject.toFlowable(BackpressureStrategy.LATEST);
  }

  public Completable enableLocationUpdates() {
    Log.d(TAG, "Attempting to enable location updates");
    return permissionsManager
        .obtainFineLocationPermission()
        .andThen(enableLocationSettings())
        .andThen(requestLocationUpdates());
  }

  @SuppressLint("MissingPermission")
  private Completable requestLocationUpdates() {
    Log.d(TAG, "Requesting location updates");
    locationCallback = new LocationCallbackImpl();
    return RxTask.toCompletable(
      getFusedLocationProviderClient(context)
        .requestLocationUpdates(
          FINE_LOCATION_UPDATES_REQUEST, locationCallback, Looper.myLooper()))
      .doOnComplete(
        () -> {
          Log.d(TAG, "requestLocationUpdates() successful");
          // Requesting last location rather than waiting for next update usually gives
          // the user a quicker response when enabling location lock. This will fail, however,
          // immediately after enabling location settings, in which case just ignore the failure
          // and wait for the next location update.
          lastLocation().subscribe(locationUpdateSubject::onNext, t -> {
          });
        });
  }

  // TODO: Request/remove updates on resume/pause.
  public Completable disableLocationUpdates() {
    if (locationCallback != null) {
      getFusedLocationProviderClient(context).removeLocationUpdates(locationCallback);
      locationCallback = null;
    }
    return Completable.complete();
  }

  @SuppressLint("MissingPermission")
  public Single<Point> lastLocation() {
    return Single.create(
        src -> {
          Log.d(TAG, "Requesting last known location");
          getFusedLocationProviderClient(context)
              .getLastLocation()
              .addOnSuccessListener(l -> onGetLastLocationSuccess(l, src))
              .addOnFailureListener(e -> src.onError(e));
        });
  }

  @SuppressLint("MissingPermission")
  private void onGetLastLocationSuccess(Location location, SingleEmitter<Point> emitter) {
    if (location == null) {
      // NOTE: This is will usually occur right after turning on location settings.
      Log.d(TAG, "Last known location null");
      emitter.onError(new NullPointerException());
    } else {
      Log.d(TAG, "Got last known location");
      emitter.onSuccess(toPoint(location));
    }
  }

  private class LocationCallbackImpl extends LocationCallback {

    @Override
    public void onLocationResult(LocationResult locationResult) {
      Location lastLocation = locationResult.getLastLocation();
      Log.v(TAG, lastLocation.toString());
      locationUpdateSubject.onNext(toPoint(lastLocation));
    }

    @Override
    public void onLocationAvailability(LocationAvailability locationAvailability) {
      if (!locationAvailability.isLocationAvailable()) {
        Log.d(TAG, "Location unavailable");
        // TODO: Warn user and disable location lock.
      }
    }
  }
}
