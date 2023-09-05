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

import com.google.android.ground.Config.DEFAULT_LOI_ZOOM_LEVEL
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.model.Survey
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.system.LocationManager
import com.google.android.ground.ui.map.gms.GmsExt.toBounds
import com.google.android.ground.ui.map.gms.toCoordinates
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.withIndex

@Singleton
class MapController
@Inject
constructor(
  private val locationManager: LocationManager,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val surveyRepository: SurveyRepository,
  private val mapStateRepository: MapStateRepository,
  @ApplicationScope private val externalScope: CoroutineScope
) {

  private val _cameraUpdates = MutableStateFlow<CameraPosition?>(null)

  /** Emits a combined stream of camera update requests. */
  fun getCameraUpdates(): SharedFlow<CameraPosition> =
    merge(
        _cameraUpdates.filterNotNull(),
        getCameraUpdatesFromLocationChanges(),
        getCameraUpdatedFromSurveyChanges(),
      )
      .shareIn(externalScope, replay = 1, started = SharingStarted.Eagerly)

  /** Emits a stream of camera update requests due to location changes. */
  private fun getCameraUpdatesFromLocationChanges(): Flow<CameraPosition> {
    val locationUpdates = locationManager.locationUpdates.map { it.toCoordinates() }
    // The first update pans and zooms the camera to the appropriate zoom level;
    // subsequent ones only pan the map.
    return locationUpdates.withIndex().transform { (index, location) ->
      emit(
        if (index == 0) CameraPosition(location, DEFAULT_LOI_ZOOM_LEVEL)
        else CameraPosition(location)
      )
    }
  }

  /** Emits a stream of camera update requests due to active survey changes. */
  private fun getCameraUpdatedFromSurveyChanges(): Flow<CameraPosition> =
    surveyRepository.activeSurveyFlow.filterNotNull().transform {
      getLastSavedPositionOrDefaultBounds(it)?.let { position -> emit(position) }
    }

  private suspend fun getLastSavedPositionOrDefaultBounds(survey: Survey): CameraPosition? {
    // Attempt to fetch last saved position from local storage.
    val savedPosition = mapStateRepository.getCameraPosition(survey.id)?.copy(isAllowZoomOut = true)
    if (savedPosition != null) {
      return savedPosition
    }

    // Compute the default viewport which includes all LOIs in the given survey.
    val geometries = locationOfInterestRepository.getAllGeometries(survey)
    return geometries.toBounds()?.let { CameraPosition(bounds = it) }
  }

  /** Requests moving the map camera to [position] with zoom level [DEFAULT_LOI_ZOOM_LEVEL]. */
  fun panAndZoomCamera(position: Coordinates) {
    _cameraUpdates.value = CameraPosition(position, DEFAULT_LOI_ZOOM_LEVEL)
  }
}
