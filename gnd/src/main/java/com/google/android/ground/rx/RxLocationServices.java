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

package com.google.android.ground.rx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observer;
import io.reactivex.Single;

/** Thin wrapper around LocationServices exposing key features as reactive streams. */
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
    private final FusedLocationProviderClient fusedLocationProviderClient;

    private RxFusedLocationProviderClient(Context context) {
      this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public Maybe<Location> getLastLocation() {
      return RxTask.toMaybe(() -> fusedLocationProviderClient.getLastLocation());
    }

    @SuppressLint("MissingPermission")
    public Completable requestLocationUpdates(
        LocationRequest locationRequest, RxLocationCallback locationCallback) {
      return RxTask.toCompletable(
          () ->
              fusedLocationProviderClient.requestLocationUpdates(
                  locationRequest, locationCallback, Looper.myLooper()));
    }

    public Completable removeLocationUpdates(RxLocationCallback locationCallback) {
      return RxTask.toCompletable(
          () -> fusedLocationProviderClient.removeLocationUpdates(locationCallback));
    }

    public static class RxLocationCallback extends LocationCallback {
      private final Observer<Location> locationObserver;

      private RxLocationCallback(Observer<Location> locationObserver) {
        this.locationObserver = locationObserver;
      }

      public static RxLocationCallback create(Observer<Location> locationObserver) {
        return new RxLocationCallback(locationObserver);
      }

      @Override
      public void onLocationResult(LocationResult locationResult) {
        locationObserver.onNext(locationResult.getLastLocation());
      }

      @Override
      public void onLocationAvailability(LocationAvailability locationAvailability) {
        // This happens sometimes when GPS signal is temporarily lost.
      }
    }
  }
}
