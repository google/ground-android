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
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.google.android.ground.Config.CLUSTERING_ZOOM_THRESHOLD
import com.google.android.ground.Config.ZOOM_LEVEL_THRESHOLD
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.*
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
import com.google.android.ground.ui.map.MapController
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import timber.log.Timber

@SharedViewModel
class HomeScreenMapContainerViewModel
@Inject
internal constructor(
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val mapController: MapController,
  private val mapStateRepository: MapStateRepository,
  private val submissionRepository: SubmissionRepository,
  locationManager: LocationManager,
  settingsManager: SettingsManager,
  offlineAreaRepository: OfflineAreaRepository,
  permissionsManager: PermissionsManager,
  surveyRepository: SurveyRepository,
) :
  BaseMapViewModel(
    locationManager,
    mapStateRepository,
    settingsManager,
    offlineAreaRepository,
    permissionsManager,
    mapController,
    surveyRepository
  ) {

  val mapLocationOfInterestFeatures: LiveData<Set<Feature>>

  private var lastCameraPosition: CameraPosition? = null

  private val tileProviders: MutableList<MapBoxOfflineTileProvider> = ArrayList()

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
    // into the repo
    // LOIs that are persisted to the local and remote dbs.
    mapLocationOfInterestFeatures =
      surveyRepository.activeSurveyFlowable
        .switchMap { survey ->
          if (survey.isPresent)
            locationOfInterestRepository
              .findLocationsOfInterest(survey.get())
              .map { toLocationOfInterestFeatures(it) }
              .asFlowable()
              .startWith(setOf<Feature>())
              .distinctUntilChanged()
          else Flowable.just(setOf())
        }
        .toLiveData()

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
      surveyRepository.activeSurveyFlow.map {
        it?.jobs?.filter { job -> job.suggestLoiTaskType != null }?.toList() ?: listOf()
      }.distinctUntilChanged()
  }

  private suspend fun toLocationOfInterestFeatures(
    locationsOfInterest: Set<LocationOfInterest>
  ): Set<Feature> = // TODO: Add support for polylines similar to mapPins.
  locationsOfInterest
      .map {
        val submissionCount = submissionRepository.getSubmissions(it).size
        Feature(
          id = it.id,
          type = FeatureType.LOCATION_OF_INTEREST.ordinal,
          flag = submissionCount > 0,
          geometry = it.geometry
        )
      }
      .toPersistentSet()

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
      mapController.panAndZoomCamera(geometry.coordinates)
    }
  }

  fun panAndZoomCamera(position: Point) {
    mapController.panAndZoomCamera(position.coordinates)
  }

  // TODO(#691): Create our own wrapper/interface for MbTiles providers.
  fun queueTileProvider(tileProvider: MapBoxOfflineTileProvider) {
    tileProviders.add(tileProvider)
  }

  fun closeProviders() {
    tileProviders.forEach { it.close() }
  }

  fun getZoomThresholdCrossed(): Observable<Nil> = zoomThresholdCrossed
}
