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
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.system.channel.ChannelLocationCallback
import com.google.android.ground.system.rx.RxFusedLocationProviderClient
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber

private const val UPDATE_INTERVAL: Long = 1000 /* 1 sec */

private const val FASTEST_INTERVAL: Long = 250 /* 250 ms */

val FINE_LOCATION_UPDATES_REQUEST: LocationRequest =
  LocationRequest()
    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
    .setInterval(UPDATE_INTERVAL)
    .setFastestInterval(FASTEST_INTERVAL)

@Singleton
class LocationManager
@Inject
constructor(
  @ApplicationScope private val externalScope: CoroutineScope,
  private val locationClient: RxFusedLocationProviderClient,
) {

  private val locationUpdates = Channel<Location>()
  private val latestLocationCallback = ChannelLocationCallback(locationUpdates, externalScope)

  /**
   * Returns the location StateFlow. New subscribers and downstream subscribers that can't keep up
   * will only see the latest location. Returns null if location lock is disabled
   */
  fun getLatestLocation(): Flow<Location?> = locationUpdates.receiveAsFlow()

  fun requestLocationUpdates() =
    locationClient.requestLocationUpdates(FINE_LOCATION_UPDATES_REQUEST, latestLocationCallback)

  // TODO: Request/remove updates on resume/pause.
  @Synchronized
  fun disableLocationUpdates(): Single<Result<Boolean>> =
    removeLocationUpdates()
      .toSingle { Result.success(false) }
      // Ignore errors as they are usually caused by disabling the same callback multiple times.
      .doOnError { Timber.e(it, "disableLocationUpdates") }
      .onErrorReturn { Result.success(false) }

  private fun removeLocationUpdates() = locationClient.removeLocationUpdates(latestLocationCallback)
}
