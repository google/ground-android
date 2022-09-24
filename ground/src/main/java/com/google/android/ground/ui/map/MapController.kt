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

import com.google.android.ground.model.geometry.Point
import com.google.android.ground.rx.annotations.Hot
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapController @Inject constructor(private val locationController: LocationController) {

  private val cameraUpdatesSubject: @Hot Subject<CameraUpdate> = PublishSubject.create()

  /** Emits a combined stream of camera update requests. */
  fun getCameraUpdates(): Flowable<CameraUpdate> =
    cameraUpdatesSubject
      .toFlowable(BackpressureStrategy.LATEST)
      .mergeWith(getCameraUpdatesFromLocationChanges())

  /** Emits a stream of camera update requests due to location changes. */
  private fun getCameraUpdatesFromLocationChanges(): Flowable<CameraUpdate> {
    val locationUpdates = locationController.getLocationUpdates()
    // The first update pans and zooms the camera to the appropriate zoom level;
    // subsequent ones only pan the map.
    return locationUpdates
      .take(1)
      .map { CameraUpdate.panAndZoomIn(it) }
      .concatWith(locationUpdates.map { CameraUpdate.pan(it) }.skip(1))
  }

  fun panAndZoomCamera(cameraPosition: CameraPosition) {
    cameraUpdatesSubject.onNext(CameraUpdate.panAndZoom(cameraPosition))
  }

  fun panAndZoomCamera(position: Point) {
    cameraUpdatesSubject.onNext(CameraUpdate.panAndZoomIn(position))
  }
}
