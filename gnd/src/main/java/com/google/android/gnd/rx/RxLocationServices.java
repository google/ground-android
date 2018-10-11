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

package com.google.android.gnd.rx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;

/** Exposes select Android Location Services using Rx Observables. */
public class RxLocationServices {
  public static RxSettingsClient getSettingsClient(Context context) {
    return new RxSettingsClient(context);
  }

  public static RxFusedLocationProviderClient getFusedLocationProviderClient(Context context) {
    return new RxFusedLocationProviderClient(context);
  }

  public static class RxSettingsClient {

    private final SettingsClient settingsClient;

    private RxSettingsClient(Context context) {
      this.settingsClient = LocationServices.getSettingsClient(context);
    }

    public Single<LocationSettingsResponse> checkLocationSettings(
        LocationSettingsRequest settingsRequest) {
      return RxTask.toSingle(() -> settingsClient.checkLocationSettings(settingsRequest));
    }
  }

  public static class RxFusedLocationProviderClient {
    private final String TAG = RxFusedLocationProviderClient.class.getSimpleName();

    private final FusedLocationProviderClient fusedLocationProviderClient;
    private RxLocationCallback locationCallback;
    private BehaviorSubject<Location> locationUpdateSubject = BehaviorSubject.create();

    private RxFusedLocationProviderClient(Context context) {
      this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public Maybe<Location> getLastLocation() {
      return RxTask.toMaybe(() -> fusedLocationProviderClient.getLastLocation());
    }

    public Completable requestLocationUpdates(LocationRequest locationRequest) {
      return RxTask.toCompletable(() -> requestLocationUpdatesInternal(locationRequest));
    }

    @SuppressLint("MissingPermission")
    private synchronized Task requestLocationUpdatesInternal(LocationRequest locationRequest) {
      Log.d(TAG, "Requesting location updates");
      locationCallback = new RxLocationCallback();
      return fusedLocationProviderClient.requestLocationUpdates(
          locationRequest, locationCallback, Looper.myLooper());
    }

    public Flowable<Location> getLocationUpdates() {
      return locationUpdateSubject.toFlowable(BackpressureStrategy.LATEST);
    }

    public synchronized Completable removeLocationUpdates() {
      if (locationCallback == null) {
        return Completable.complete();
      }
      return RxTask.toCompletable(
              () -> fusedLocationProviderClient.removeLocationUpdates(locationCallback))
          .doOnComplete(() -> locationCallback = null);
    }

    private class RxLocationCallback extends LocationCallback {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        Location lastLocation = locationResult.getLastLocation();
        Log.v(TAG, lastLocation.toString());
        locationUpdateSubject.onNext(lastLocation);
      }

      @Override
      public void onLocationAvailability(LocationAvailability locationAvailability) {
        // This happens sometimes when GPS signal is temporarily lost.
        if (!locationAvailability.isLocationAvailable()) {
          Log.v(TAG, "Location unavailable");
        }
      }
    }
  }
}
