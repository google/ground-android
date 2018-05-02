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
import android.support.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;

import io.reactivex.Completable;
import io.reactivex.Single;

/** Exposes select Android Location Services using Rx Observables. */
public class RxLocationServices {

  public static RxSettingsClient getSettingsClient(Context context) {
    return new RxSettingsClient(context);
  }

  public static RxFusedLocationProviderClient getFusedLocationProviderClient(Context context) {
    return new RxFusedLocationProviderClient(context);
  }

  public static class RxSettingsClient {

    private final Single<SettingsClient> settingsClient;

    private RxSettingsClient(Context context) {
      this.settingsClient = Single.fromCallable(() -> LocationServices.getSettingsClient(context));
    }

    public Single<LocationSettingsResponse> checkLocationSettings(
        LocationSettingsRequest settingsRequest) {
      return settingsClient
        .map(client -> client.checkLocationSettings(settingsRequest))
        .flatMap(RxTask::toSingle);
    }
  }

  public static class RxFusedLocationProviderClient {

    private final Single<FusedLocationProviderClient> fusedLocationProviderClient;

    private RxFusedLocationProviderClient(Context context) {
      this.fusedLocationProviderClient =
        Single.fromCallable(() -> LocationServices.getFusedLocationProviderClient(context));
    }

    @SuppressLint("MissingPermission")
    public Single<Location> getLastLocation() {
      return fusedLocationProviderClient
        .map(FusedLocationProviderClient::getLastLocation)
        .flatMap(RxTask::toSingle);
    }

    @SuppressLint("MissingPermission")
    public Completable requestLocationUpdates(
        LocationRequest locationRequest,
        @NonNull LocationCallback locationCallback,
        Looper looper) {
      return fusedLocationProviderClient
        .map(client -> client.requestLocationUpdates(locationRequest, locationCallback, looper))
        .flatMapCompletable(RxTask::toCompletable);
    }
  }
}
