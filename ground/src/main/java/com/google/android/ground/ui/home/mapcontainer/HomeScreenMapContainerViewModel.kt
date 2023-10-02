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
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.job.getDefaultColor
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SubmissionRepository
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
import com.google.android.ground.ui.map.FeatureType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@SharedViewModel
class HomeScreenMapContainerViewModel
@Inject
internal constructor(
  private val loiRepository: LocationOfInterestRepository,
  private val mapStateRepository: MapStateRepository,
  private val submissionRepository: SubmissionRepository,
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
    loiRepository,
    ioDispatcher
  ) {

  /** Set of [Feature] to render on the map. */
  val mapLoiFeatures: Flow<Set<Feature>>

  /**
   * List of [LocationOfInterest] for the active survey that are present within the viewport and
   * zoom level is clustering threshold or higher.
   */
  val loisInViewport: StateFlow<List<LocationOfInterest>>

  /** [LocationOfInterest] clicked by the user. */
  val loiClicks: MutableStateFlow<LocationOfInterest?> = MutableStateFlow(null)

  /**
   * List of [Job] within the active survey of `suggestLoiType` and zoom level is clustering
   * threshold or higher.
   */
  val suggestLoiJobs: Flow<List<Job>>

  /* UI Clicks */
  private val zoomThresholdCrossed: @Hot Subject<Nil> = PublishSubject.create()

  init {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE
    // TODO: Clear location of interest markers when survey is deactivated.
    // TODO: Since we depend on survey stream from repo anyway, this transformation can be moved
    //  into the repository.

    val activeSurvey = surveyRepository.activeSurveyFlow.distinctUntilChanged()

    mapLoiFeatures =
      activeSurvey.flatMapLatest {
        if (it == null) flowOf(setOf()) else getLocationOfInterestFeatures(it)
      }

    val isZoomedInFlow =
      getCurrentCameraPosition().mapNotNull { it.zoomLevel }.map { it >= CLUSTERING_ZOOM_THRESHOLD }

    loisInViewport =
      activeSurvey
        .combine(isZoomedInFlow) { survey, isZoomedIn -> Pair(survey, isZoomedIn) }
        .flatMapLatest { (survey, isZoomedIn) ->
          val bounds = currentCameraPosition.value?.bounds
          if (bounds == null || survey == null || !isZoomedIn) flowOf(listOf())
          else loiRepository.getWithinBoundsOnceAndStream(survey, bounds).asFlow()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    suggestLoiJobs =
      activeSurvey
        .combine(isZoomedInFlow) { survey, isZoomedIn -> Pair(survey, isZoomedIn) }
        .flatMapLatest { (survey, isZoomedIn) ->
          flowOf(
            if (survey == null || !isZoomedIn) listOf()
            else survey.jobs.filter { it.suggestLoiTaskType != null }
          )
        }
  }

  override fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    super.onMapCameraMoved(newCameraPosition)
    Timber.d("Setting position to $newCameraPosition")
    onZoomChange(lastCameraPosition?.zoomLevel, newCameraPosition.zoomLevel)
    mapStateRepository.setCameraPosition(newCameraPosition)
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
  fun onFeatureClicked(features: Set<Feature>) {
    val geometry = features.first().geometry
    for (loi in loisInViewport.value) {
      if (loi.geometry == geometry) {
        loiClicks.value = loi
      }
    }
  }

  fun getZoomThresholdCrossed(): Observable<Nil> = zoomThresholdCrossed

  private fun getLocationOfInterestFeatures(survey: Survey): Flow<Set<Feature>> =
    loiRepository.getLocationsOfInterest(survey).map {
      it.map { loi -> loi.toFeature() }.toPersistentSet()
    }

  private suspend fun LocationOfInterest.toFeature() =
    Feature(
      id = id,
      type = FeatureType.LOCATION_OF_INTEREST.ordinal,
      flag =
        submissionCount + submissionRepository.getPendingCreateCount(id) -
          submissionRepository.getPendingDeleteCount(id) > 0,
      geometry = geometry,
      style = Feature.Style(job.getDefaultColor()),
      clusterable = true
    )
}
