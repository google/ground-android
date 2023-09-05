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
package com.google.android.ground.ui.home.mapcontainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.Config.CLUSTERING_ZOOM_THRESHOLD
import com.google.android.ground.Config.ZOOM_LEVEL_THRESHOLD
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.Feature
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber

@SharedViewModel
class HomeScreenMapContainerViewModel
@Inject
internal constructor(
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val mapStateRepository: MapStateRepository,
  locationManager: LocationManager,
  settingsManager: SettingsManager,
  offlineAreaRepository: OfflineAreaRepository,
  permissionsManager: PermissionsManager,
  surveyRepository: SurveyRepository,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) :
  BaseMapViewModel(
    locationManager,
    mapStateRepository,
    settingsManager,
    offlineAreaRepository,
    permissionsManager,
    surveyRepository,
    locationOfInterestRepository,
    ioDispatcher
  ) {

  val mapLocationOfInterestFeatures: StateFlow<Set<Feature>>

  private var lastCameraPosition: CameraPosition? = null

  /* UI Clicks */
  private val zoomThresholdCrossed: @Hot Subject<Nil> = PublishSubject.create()

  /**
   * List of [LocationOfInterest] for the active survey that are present within the map bounds and
   * zoom level is clustering threshold or higher.
   */
  val loisWithinMapBoundsAtVisibleZoomLevel: LiveData<List<LocationOfInterest>>

  val suggestLoiJobs: Flow<List<Job>>

  init {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE
    // TODO: Clear location of interest markers when survey is deactivated.
    // TODO: Since we depend on survey stream from repo anyway, this transformation can be moved
    //  into the repository.

    // LOIs that are persisted to the local and remote dbs.
    mapLocationOfInterestFeatures =
      surveyRepository.activeSurveyFlow
        .transform { survey ->
          if (survey == null) {
            emit(setOf())
          } else {
            emitAll(locationOfInterestRepository.findLocationsOfInterestFeatures(survey))
          }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, setOf())

    loisWithinMapBoundsAtVisibleZoomLevel =
      surveyRepository.activeSurveyFlowable
        .switchMap { survey ->
          cameraZoomUpdates.switchMap { zoomLevel ->
            if (zoomLevel >= CLUSTERING_ZOOM_THRESHOLD && survey.isPresent)
              locationOfInterestRepository.getWithinBoundsOnceAndStream(
                survey.get(),
                cameraBoundUpdates
              )
            else Flowable.just(listOf())
          }
        }
        .toLiveData()

    suggestLoiJobs =
      surveyRepository.activeSurveyFlow
        .combine(cameraZoomUpdates.asFlow()) { survey, zoomLevel ->
          if (zoomLevel < CLUSTERING_ZOOM_THRESHOLD) {
            listOf()
          } else {
            survey?.jobs?.filter { job -> job.suggestLoiTaskType != null }?.toList() ?: listOf()
          }
        }
        .distinctUntilChanged()
  }

  override fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    super.onMapCameraMoved(newCameraPosition)
    Timber.d("Setting position to $newCameraPosition")
    onZoomChange(lastCameraPosition?.zoomLevel, newCameraPosition.zoomLevel)
    mapStateRepository.setCameraPosition(newCameraPosition)
    lastCameraPosition = newCameraPosition
  }

  private fun onZoomChange(oldZoomLevel: Float?, newZoomLevel: Float?) {
    if (oldZoomLevel == null || newZoomLevel == null) return

    val zoomThresholdCrossed =
      oldZoomLevel < ZOOM_LEVEL_THRESHOLD && newZoomLevel >= ZOOM_LEVEL_THRESHOLD ||
        oldZoomLevel >= ZOOM_LEVEL_THRESHOLD && newZoomLevel < ZOOM_LEVEL_THRESHOLD
    if (zoomThresholdCrossed) {
      this.zoomThresholdCrossed.onNext(Nil.NIL)
    }
  }

  /**
   * Intended as a callback for when a specific map [Feature] is clicked. If the click is ambiguous,
   * (list of features > 1), it chooses the first [Feature].
   */
  fun onFeatureClick(features: List<Feature>) {
    val geometry = features[0].geometry

    if (geometry is Point) {
      panAndZoomCamera(geometry.coordinates)
    }
  }

  fun getZoomThresholdCrossed(): Observable<Nil> = zoomThresholdCrossed
}
