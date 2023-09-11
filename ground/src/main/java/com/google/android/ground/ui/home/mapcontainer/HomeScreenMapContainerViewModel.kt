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

import androidx.lifecycle.viewModelScope
import com.google.android.ground.Config.CLUSTERING_ZOOM_THRESHOLD
import com.google.android.ground.Config.ZOOM_LEVEL_THRESHOLD
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.Survey
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
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

  private var _survey: Survey? = null

  private val _mapLoiFeatures: MutableStateFlow<Set<Feature>> = MutableStateFlow(setOf())
  val mapLoiFeatures: StateFlow<Set<Feature>> =
    _mapLoiFeatures.stateIn(viewModelScope, SharingStarted.Lazily, setOf())

  /* UI Clicks */
  private val zoomThresholdCrossed: @Hot Subject<Nil> = PublishSubject.create()

  /**
   * List of [LocationOfInterest] for the active survey that are present within the viewport and
   * zoom level is clustering threshold or higher.
   */
  private val _loisInViewportAtVisibleZoomLevel: MutableStateFlow<List<LocationOfInterest>> =
    MutableStateFlow(listOf())
  val loisInViewportAtVisibleZoomLevel: StateFlow<List<LocationOfInterest>> =
    _loisInViewportAtVisibleZoomLevel.stateIn(viewModelScope, SharingStarted.Lazily, listOf())

  private val _suggestLoiJobs: MutableStateFlow<List<Job>> = MutableStateFlow(listOf())
  val suggestLoiJobs: StateFlow<List<Job>> =
    _suggestLoiJobs.stateIn(viewModelScope, SharingStarted.Lazily, listOf())

  init {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE
    // TODO: Clear location of interest markers when survey is deactivated.
    // TODO: Since we depend on survey stream from repo anyway, this transformation can be moved
    //  into the repository.

    viewModelScope.launch {
      surveyRepository.activeSurveyFlow.collect {
        _survey = it
        refreshMapFeaturesAndCards(it)
      }
    }
  }

  private suspend fun refreshMapFeaturesAndCards(survey: Survey?) {
    updateMapFeatures(survey)
    updateLoisAndJobs(survey, currentCameraPosition)
  }

  private suspend fun updateLoisAndJobs(survey: Survey?, cameraPosition: CameraPosition?) {
    updateMapLois(survey, cameraPosition)
    updateSuggestLoiJobs(survey, cameraPosition)
  }

  private suspend fun updateMapFeatures(survey: Survey?) {
    if (survey == null) {
      _mapLoiFeatures.value = setOf()
    } else {
      // LOIs that are persisted to the local and remote dbs.
      _mapLoiFeatures.emitAll(locationOfInterestRepository.findLocationsOfInterestFeatures(survey))
    }
  }

  private suspend fun updateMapLois(survey: Survey?, cameraPosition: CameraPosition?) {
    val bounds = cameraPosition?.bounds
    if (bounds == null || survey == null || cameraPosition.isBelowClusteringZoomThreshold()) {
      _loisInViewportAtVisibleZoomLevel.value = listOf()
    } else {
      _loisInViewportAtVisibleZoomLevel.emitAll(
        locationOfInterestRepository.getWithinBoundsOnceAndStream(survey, bounds).asFlow()
      )
    }
  }

  private fun updateSuggestLoiJobs(survey: Survey?, cameraPosition: CameraPosition?) {
    if (survey == null || cameraPosition.isBelowClusteringZoomThreshold()) {
      _suggestLoiJobs.value = listOf()
    } else {
      _suggestLoiJobs.value = survey.jobs.filter { it.suggestLoiTaskType != null }.toList()
    }
  }

  override fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    super.onMapCameraMoved(newCameraPosition)
    Timber.d("Setting position to $newCameraPosition")
    onZoomChange(lastCameraPosition?.zoomLevel, newCameraPosition.zoomLevel)
    mapStateRepository.setCameraPosition(newCameraPosition)

    viewModelScope.launch { updateLoisAndJobs(_survey, newCameraPosition) }
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

  private fun CameraPosition?.isBelowClusteringZoomThreshold() =
    this?.zoomLevel?.let { it < CLUSTERING_ZOOM_THRESHOLD } ?: true
}
