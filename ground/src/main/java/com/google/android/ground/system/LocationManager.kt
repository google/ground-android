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
package com.google.android.ground.system

import android.location.Location
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.system.channel.LocationSharedFlowCallback
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

private const val UPDATE_INTERVAL: Long = 1000 /* 1 sec */

private const val FASTEST_INTERVAL: Long = 250 /* 250 ms */

val FINE_LOCATION_UPDATES_REQUEST: LocationRequest =
  LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL)
    .apply { setMinUpdateIntervalMillis(FASTEST_INTERVAL) }
    .build()

@Singleton
class LocationManager
@Inject
constructor(
  @ApplicationScope private val externalScope: CoroutineScope,
  private val locationClient: FusedLocationProviderClient,
) {

  private val _locationUpdates = MutableSharedFlow<Location>(replay = 1)
  val locationUpdates: SharedFlow<Location>
    get() = _locationUpdates

  private val locationCallback = LocationSharedFlowCallback(_locationUpdates, externalScope)

  // TODO: Request updates on resume.
  // Issue URL: https://github.com/google/ground-android/issues/2624
  /** Immediately emits the last known location (if any) and then subscribes to location updates. */
  suspend fun requestLocationUpdates() {
    locationClient.getLastLocation()?.let { _locationUpdates.emit(it) }
    locationClient.requestLocationUpdates(FINE_LOCATION_UPDATES_REQUEST, locationCallback)
  }

  // TODO: Remove updates on pause.
  // Issue URL: https://github.com/google/ground-android/issues/2624
  suspend fun disableLocationUpdates() = locationClient.removeLocationUpdates(locationCallback)
}
