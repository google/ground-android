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
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.rx.RxCompletable.completeIf
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.rx.RxFusedLocationProviderClient
import com.google.android.ground.system.rx.RxLocationCallback
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
  private val permissionsManager: PermissionsManager,
  private val settingsManager: SettingsManager,
  private val locationClient: RxFusedLocationProviderClient,
  private val mapStateRepository: MapStateRepository
) {

  private val locationUpdates: @Hot(replays = true) Subject<Location> = BehaviorSubject.create()
  private val locationUpdateCallback: RxLocationCallback = RxLocationCallback(locationUpdates)
  val locationLockState: StateFlow<Result<Boolean>> =
    MutableStateFlow(Result.success(mapStateRepository.isLocationLockEnabled))

  /**
   * Returns the location update stream. New subscribers and downstream subscribers that can't keep
   * up will only see the latest location.
   */
  fun getLocationUpdates(): Flowable<Location> =
    locationUpdates
      // There sometimes noticeable latency between when location update request succeeds and when
      // the first location update is received. Requesting the last know location is usually
      // immediate, so we merge into the stream to reduce perceived latency.
      .startWith(locationClient.lastLocation.toObservable())
      .toFlowable(BackpressureStrategy.LATEST)

  /**
   * Asynchronously try to enable location permissions and settings, and if successful, turns on
   * location updates exposed by [.getLocationUpdates].
   */
  @Synchronized
  fun enableLocationUpdates(): Single<Result<Boolean>> {
    Timber.d("Attempting to enable location updates")
    return permissionsManager
      .obtainPermission(permission.ACCESS_FINE_LOCATION)
      .andThen(settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST))
      .andThen(requestLocationUpdates())
      .toSingle { Result.success(true) }
      .onErrorReturn { Result.failure(it) }
  }

  fun toggleLocationLock() {
    if (locationLockState.value.getOrDefault(false)) {
      unlock()

      disableLocationUpdates()
    } else {
      permissionsManager
        .obtainPermission(permission.ACCESS_FINE_LOCATION)
        .andThen(
          completeIf {
            lock()
            true
          }
        )
        .andThen(settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST))
        .andThen(requestLocationUpdates())
        .toSingle {
          lock()
          Result.success(true)
        }
        .onErrorReturn { Result.failure(it) }
        .subscribe()
    }
  }

  private fun lock() = onLockStateChanged(true)

  /** Releases location lock by disabling location updates. */
  private fun unlock() = onLockStateChanged(false)

  private fun onLockStateChanged(isLocked: Boolean) {
    mapStateRepository.isLocationLockEnabled = isLocked
  }

  private fun requestLocationUpdates() =
    locationClient.requestLocationUpdates(FINE_LOCATION_UPDATES_REQUEST, locationUpdateCallback)

  // TODO: Request/remove updates on resume/pause.
  @Synchronized
  fun disableLocationUpdates(): Single<Result<Boolean>> =
    removeLocationUpdates()
      .toSingle { Result.success(false) }
      // Ignore errors as they are usually caused by disabling the same callback multiple times.
      .doOnError { Timber.e(it, "disableLocationUpdates") }
      .onErrorReturn { Result.success(false) }

  private fun removeLocationUpdates() = locationClient.removeLocationUpdates(locationUpdateCallback)
}
