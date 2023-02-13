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

import android.Manifest.permission
import android.location.Location
import com.google.android.gms.location.LocationRequest
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.system.rx.RxFusedLocationProviderClient
import com.google.android.ground.system.stateflow.StateFlowLocationCallback
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.await
import timber.log.Timber

private const val UPDATE_INTERVAL: Long = 1000 /* 1 sec */

private const val FASTEST_INTERVAL: Long = 250 /* 250 ms */

private val FINE_LOCATION_UPDATES_REQUEST =
  LocationRequest()
    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
    .setInterval(UPDATE_INTERVAL)
    .setFastestInterval(FASTEST_INTERVAL)

@Singleton
class LocationManager
@Inject
constructor(
  @ApplicationScope private val externalScope: CoroutineScope,
  private val permissionsManager: PermissionsManager,
  private val settingsManager: SettingsManager,
  private val locationClient: RxFusedLocationProviderClient,
  private val mapStateRepository: MapStateRepository
) {

  val locationLockState: MutableStateFlow<Result<Boolean>> =
    MutableStateFlow(Result.success(mapStateRepository.isLocationLockEnabled))
  private val locationUpdatesStateFlow: MutableStateFlow<Location?> = MutableStateFlow(null)
  private val locationUpdateStateFlowCallback = StateFlowLocationCallback(locationUpdatesStateFlow)

  private val locationLockAwareLocationUpdates: StateFlow<Location?> =
    locationUpdatesStateFlow
      .combine(locationLockState) { location, lockState ->
        if (lockState.getOrDefault(false)) {
          location
        } else {
          null
        }
      }
      .stateIn(externalScope, SharingStarted.Lazily, null)

  /**
   * Returns the location update stream. New subscribers and downstream subscribers that can't keep
   * up will only see the latest location.
   */
  fun getLocationUpdates(): StateFlow<Location?> = locationLockAwareLocationUpdates

  suspend fun toggleLocationLock() {
    if (locationLockState.value.getOrDefault(false)) {
      disableLocationLock()

      disableLocationUpdates()
    } else {
      try {
        permissionsManager.obtainPermission(permission.ACCESS_FINE_LOCATION).await()

        settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST).await()

        enableLocationLock()

        requestLocationUpdates().await()
      } catch (e: PermissionDeniedException) {
        locationLockState.value = Result.failure(e)
      }
    }
  }

  private fun enableLocationLock() = onLockStateChanged(true)

  /** Releases location enableLocationLock by disabling location updates. */
  fun disableLocationLock() = onLockStateChanged(false)

  private fun onLockStateChanged(isLocked: Boolean) {
    locationLockState.value = Result.success(isLocked)
    mapStateRepository.isLocationLockEnabled = isLocked
  }

  private fun requestLocationUpdates() =
    locationClient.requestLocationUpdates(
      FINE_LOCATION_UPDATES_REQUEST,
      locationUpdateStateFlowCallback
    )

  // TODO: Request/remove updates on resume/pause.
  @Synchronized
  fun disableLocationUpdates(): Single<Result<Boolean>> =
    removeLocationUpdates()
      .toSingle { Result.success(false) }
      // Ignore errors as they are usually caused by disabling the same callback multiple times.
      .doOnError { Timber.e(it, "disableLocationUpdates") }
      .onErrorReturn { Result.success(false) }

  private fun removeLocationUpdates() =
    locationClient.removeLocationUpdates(locationUpdateStateFlowCallback)
}
