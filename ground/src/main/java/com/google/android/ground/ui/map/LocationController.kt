/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.map

import android.location.Location
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.LocationManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationController @Inject constructor(private val locationManager: LocationManager) {

  private val locationLockChangeRequests: @Hot Subject<Boolean> = PublishSubject.create()

  /** Emits a stream of location lock requests. */
  fun getLocationLockUpdates(): Flowable<Result<Boolean>> =
    locationLockChangeRequests
      .switchMapSingle { toggleLocationUpdates(it) }
      .toFlowable(BackpressureStrategy.LATEST)
      .share()

  fun getLocationUpdates(): Flowable<Point> =
    getLocationLockUpdates()
      .map { it.getOrDefault(false) }
      .switchMap { result: Boolean ->
        if (!result) {
          Flowable.empty()
        } else {
          locationManager.getLocationUpdates().map { it.toPoint() }
        }
      }

  private fun toggleLocationUpdates(result: Boolean) =
    if (result) locationManager.enableLocationUpdates()
    else locationManager.disableLocationUpdates()

  fun lock() = locationLockChangeRequests.onNext(true)

  fun unlock() = locationLockChangeRequests.onNext(false)

  private fun Location.toPoint(): Point = Point(Coordinate(latitude, longitude))
}
