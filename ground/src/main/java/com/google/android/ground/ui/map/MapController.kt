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
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.home.mapcontainer.MapContainerViewModel.Companion.DEFAULT_LOI_ZOOM_LEVEL
import com.google.android.ground.ui.map.gms.toPoint
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapController
@Inject
constructor(
  private val locationController: LocationController,
  private val surveyRepository: SurveyRepository
) {

  private val cameraUpdatesSubject: @Hot Subject<CameraPosition> = PublishSubject.create()

  /** Emits a combined stream of camera update requests. */
  fun getCameraUpdates(): Flowable<CameraPosition> =
    cameraUpdatesSubject
      .toFlowable(BackpressureStrategy.LATEST)
      .mergeWith(getCameraUpdatesFromLocationChanges())
      .mergeWith(getCameraUpdatedFromSurveyChanges())

  /** Emits a stream of camera update requests due to location changes. */
  private fun getCameraUpdatesFromLocationChanges(): Flowable<CameraPosition> {
    val locationUpdates = locationController.getLocationUpdates().map { it.toPoint() }
    // The first update pans and zooms the camera to the appropriate zoom level;
    // subsequent ones only pan the map.
    return locationUpdates
      .take(1)
      .map { CameraPosition(it, DEFAULT_LOI_ZOOM_LEVEL) }
      .concatWith(locationUpdates.map { CameraPosition(it) }.skip(1))
  }

  /** Emits a stream of camera update requests due to active survey changes. */
  private fun getCameraUpdatedFromSurveyChanges(): Flowable<CameraPosition> =
    surveyRepository.activeSurvey
      .filter { it.isPresent }
      .map { it.get().id }
      .flatMap { surveyId ->
        val position = surveyRepository.getLastCameraPosition(surveyId)
        if (position != null) {
          Flowable.just(position.copy(isAllowZoomOut = true))
        } else {
          Flowable.empty()
        }
      }

  /** Requests moving the map camera to [position] with zoom level [DEFAULT_LOI_ZOOM_LEVEL]. */
  fun panAndZoomCamera(position: Point) {
    cameraUpdatesSubject.onNext(CameraPosition(position, DEFAULT_LOI_ZOOM_LEVEL))
  }
}
