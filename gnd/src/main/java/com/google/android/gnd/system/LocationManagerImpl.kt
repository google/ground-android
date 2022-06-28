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
package com.google.android.gnd.system

import android.Manifest.permission
import android.location.Location
import com.google.android.gms.location.LocationRequest
import com.google.android.gnd.rx.BooleanOrError
import com.google.android.gnd.rx.BooleanOrError.Companion.falseValue
import com.google.android.gnd.rx.BooleanOrError.Companion.trueValue
import com.google.android.gnd.rx.annotations.Hot
import com.google.android.gnd.system.rx.RxFusedLocationProviderClient
import com.google.android.gnd.system.rx.RxLocationCallback
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val UPDATE_INTERVAL: Long = 1000 /* 1 sec */

private const val FASTEST_INTERVAL: Long = 250 /* 250 ms */

private val FINE_LOCATION_UPDATES_REQUEST = LocationRequest()
    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
    .setInterval(UPDATE_INTERVAL)
    .setFastestInterval(FASTEST_INTERVAL)

@Singleton
class LocationManagerImpl @Inject constructor(
    private val permissionsManager: PermissionsManager,
    private val settingsManager: SettingsManager,
    private val locationClient: RxFusedLocationProviderClient
) : LocationManager {

    private val locationUpdates: @Hot(replays = true) Subject<Location> = BehaviorSubject.create()
    private val locationUpdateCallback: RxLocationCallback =
        RxLocationCallback.create(locationUpdates)

    /**
     * Returns the location update stream. New subscribers and downstream subscribers that can't keep
     * up will only see the latest location.
     */
    override fun getLocationUpdates(): Flowable<Location> =
        locationUpdates
            // There sometimes noticeable latency between when location update request succeeds and when
            // the first location update is received. Requesting the last know location is usually
            // immediate, so we merge into the stream to reduce perceived latency.
            .startWith(locationClient.lastLocation.toObservable())
            .toFlowable(BackpressureStrategy.LATEST)

    /**
     * Asynchronously try to enable location permissions and settings, and if successful, turns on
     * location updates exposed by [getLocationUpdates].
     */
    @Synchronized
    override fun enableLocationUpdates(): Single<BooleanOrError> {
        Timber.d("Attempting to enable location updates")
        return permissionsManager
            .obtainPermission(permission.ACCESS_FINE_LOCATION)
            .andThen(settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST))
            .andThen(requestLocationUpdates())
            .toSingle { trueValue() }
            .onErrorReturn { BooleanOrError.error(it) }
    }

    private fun requestLocationUpdates() =
        locationClient.requestLocationUpdates(FINE_LOCATION_UPDATES_REQUEST, locationUpdateCallback)

    // TODO: Request/remove updates on resume/pause.
    @Synchronized
    override fun disableLocationUpdates(): Single<BooleanOrError> =
        removeLocationUpdates()
            .toSingle { falseValue() }
            // Ignore errors as they are usually caused by disabling the same callback multiple times.
            .doOnError { Timber.e(it, "disableLocationUpdates") }
            .onErrorReturn { falseValue() }

    private fun removeLocationUpdates() =
        locationClient.removeLocationUpdates(locationUpdateCallback)
}
