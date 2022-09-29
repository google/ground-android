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
package com.google.android.ground.system.rx

import android.location.Location
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import io.reactivex.Observer

/** Implementation of [LocationCallback] linked to a Reactive [Observer]. */
class RxLocationCallback(private val locationObserver: Observer<Location>) : LocationCallback() {
  override fun onLocationResult(locationResult: LocationResult) {
    locationObserver.onNext(locationResult.lastLocation)
  }

  override fun onLocationAvailability(locationAvailability: LocationAvailability) {
    // This happens sometimes when GPS signal is temporarily lost.
  }
}
