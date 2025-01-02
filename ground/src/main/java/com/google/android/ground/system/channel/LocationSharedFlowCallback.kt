/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.system.channel

import android.location.Location
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/** Implementation of [LocationCallback] linked to a [MutableSharedFlow]. */
class LocationSharedFlowCallback(
  private val locationUpdates: MutableSharedFlow<Location>,
  private val coroutineScope: CoroutineScope,
) : LocationCallback() {
  override fun onLocationResult(locationResult: LocationResult) {
    coroutineScope.launch {
      locationResult.lastLocation?.let {
        Timber.v("Location updated $it")
        locationUpdates.emit(it)
      }
    }
  }

  override fun onLocationAvailability(p0: LocationAvailability) {
    // This happens sometimes when GPS signal is temporarily lost.
  }
}
