/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.system.rx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gnd.rx.RxTask;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import javax.inject.Inject;

/**
 * Thin wrapper around {@link FusedLocationProviderClient} exposing key features as reactive
 * streams.
 */
public class RxFusedLocationProviderClient {
  private final FusedLocationProviderClient fusedLocationProviderClient;

  @Inject
  public RxFusedLocationProviderClient(Context context) {
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
}
