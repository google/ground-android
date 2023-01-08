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

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.google.android.ground.R
import com.google.android.ground.model.Survey
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.locationofinterest.LocationOfInterestType
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractMapViewModel
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.LocationController
import com.google.android.ground.ui.map.MapController
import com.google.android.ground.ui.map.MapLocationOfInterest
import com.google.android.ground.util.toImmutableSet
import com.google.common.collect.ImmutableSet
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java8.util.Optional
import javax.inject.Inject
import timber.log.Timber

@SharedViewModel
class HomeScreenMapContainerViewModel
@Inject
internal constructor(
  private val resources: Resources,
  private val surveyRepository: SurveyRepository,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val locationController: LocationController,
  private val mapController: MapController,
  offlineAreaRepository: OfflineAreaRepository
) : AbstractMapViewModel(locationController, mapController) {
  val mapLocationsOfInterest: LiveData<ImmutableSet<MapLocationOfInterest>>

  private var lastCameraPosition: CameraPosition? = null

  val mbtilesFilePaths: LiveData<ImmutableSet<String>>
  val isLocationUpdatesEnabled: LiveData<Boolean>
  val locationAccuracy: LiveData<String>
  private val tileProviders: MutableList<MapBoxOfflineTileProvider> = ArrayList()

  /** The currently selected LOI on the map. */
  private val selectedLocationOfInterest =
    BehaviorProcessor.createDefault(Optional.empty<LocationOfInterest>())

  /* UI Clicks */
  private val zoomThresholdCrossed: @Hot Subject<Nil> = PublishSubject.create()

  private fun updateSelectedLocationOfInterest(
    locationsOfInterest: ImmutableSet<MapLocationOfInterest>,
    selectedLocationOfInterest: Optional<LocationOfInterest>
  ): ImmutableSet<MapLocationOfInterest> {
    Timber.v("Updating selected LOI style")
    if (selectedLocationOfInterest.isEmpty) {
      return locationsOfInterest
    }
    val updatedLocationsOfInterest: ImmutableSet.Builder<MapLocationOfInterest> =
      ImmutableSet.builder()
    val selectedLocationOfInterestId = selectedLocationOfInterest.get().id
    for (locationOfInterest in locationsOfInterest) {
      // TODO: Update strokewidth of non MapGeoJson locationOfInterest
      updatedLocationsOfInterest.add(locationOfInterest)
    }
    return updatedLocationsOfInterest.build()
  }

  private fun toMapLocationsOfInterest(
    locationsOfInterest: ImmutableSet<LocationOfInterest>
  ): ImmutableSet<MapLocationOfInterest> {
    val points =
      locationsOfInterest
        .filter { it.type === LocationOfInterestType.POINT }
        .map { MapLocationOfInterest(it) }
        .toImmutableSet()

    // TODO: Add support for polylines similar to mapPins.
    val polygons =
      locationsOfInterest
        .filter { it.type === LocationOfInterestType.POLYGON }
        .map { MapLocationOfInterest(it) }
        .toImmutableSet()

    return ImmutableSet.builder<MapLocationOfInterest>().addAll(points).addAll(polygons).build()
  }

  private fun createLocationAccuracyFlowable() =
    locationController.getLocationUpdates().map {
      resources.getString(R.string.location_accuracy, it.accuracy)
    }

  private fun getLocationsOfInterestStream(
    activeProject: Optional<Survey>
  ): Flowable<ImmutableSet<LocationOfInterest>> =
    // Emit empty set in separate stream to force unsubscribe from LocationOfInterest updates and
    // update
    // subscribers.
    activeProject
      .map { survey: Survey ->
        locationOfInterestRepository.getLocationsOfInterestOnceAndStream(survey)
      }
      .orElse(Flowable.just(ImmutableSet.of()))

  override fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    Timber.d("Setting position to $newCameraPosition")
    onZoomChange(lastCameraPosition?.zoomLevel, newCameraPosition.zoomLevel)
    surveyRepository.setCameraPosition(surveyRepository.lastActiveSurveyId, newCameraPosition)
    lastCameraPosition = newCameraPosition
  }

  private fun onZoomChange(oldZoomLevel: Float?, newZoomLevel: Float?) {
    if (oldZoomLevel == null || newZoomLevel == null) return

    val zoomThresholdCrossed =
      (oldZoomLevel < ZOOM_LEVEL_THRESHOLD && newZoomLevel >= ZOOM_LEVEL_THRESHOLD ||
        oldZoomLevel >= ZOOM_LEVEL_THRESHOLD && newZoomLevel < ZOOM_LEVEL_THRESHOLD)
    if (zoomThresholdCrossed) {
      this.zoomThresholdCrossed.onNext(Nil.NIL)
    }
  }

  fun onMarkerClick(mapLocationOfInterest: MapLocationOfInterest) {
    val geometry = mapLocationOfInterest.locationOfInterest.geometry
    if (geometry is Point) {
      mapController.panAndZoomCamera(geometry)
    }
  }

  fun panAndZoomCamera(position: Point) {
    mapController.panAndZoomCamera(position)
  }

  // TODO(#691): Create our own wrapper/interface for MbTiles providers.
  fun queueTileProvider(tileProvider: MapBoxOfflineTileProvider) {
    tileProviders.add(tileProvider)
  }

  fun closeProviders() {
    tileProviders.forEach { it.close() }
  }

  fun getZoomThresholdCrossed(): Observable<Nil> {
    return zoomThresholdCrossed
  }

  /** Called when a LOI is (de)selected. */
  fun setSelectedLocationOfInterest(selectedLocationOfInterest: Optional<LocationOfInterest>) {
    this.selectedLocationOfInterest.onNext(selectedLocationOfInterest)
  }

  companion object {
    // Higher zoom levels means the map is more zoomed in. 0.0f is fully zoomed out.
    const val ZOOM_LEVEL_THRESHOLD = 16f
    const val DEFAULT_LOI_ZOOM_LEVEL = 18.0f

    private fun concatLocationsOfInterestSets(
      objects: Array<Any>
    ): ImmutableSet<MapLocationOfInterest> =
      listOf(*objects).flatMap { it as ImmutableSet<MapLocationOfInterest> }.toImmutableSet()
  }

  init {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE
    val locationLockStateFlowable = locationController.getLocationLockUpdates()
    isLocationUpdatesEnabled =
      LiveDataReactiveStreams.fromPublisher(
        locationLockStateFlowable.map { it.getOrDefault(false) }.startWith(false)
      )
    locationAccuracy = LiveDataReactiveStreams.fromPublisher(createLocationAccuracyFlowable())
    // TODO: Clear location of interest markers when survey is deactivated.
    // TODO: Since we depend on survey stream from repo anyway, this transformation can be moved
    // into the repo
    // LOIs that are persisted to the local and remote dbs.
    val loiStream =
      surveyRepository.activeSurvey.switchMap { activeProject ->
        getLocationsOfInterestStream(activeProject)
      }

    val savedMapLocationsOfInterest =
      Flowable.combineLatest(
        loiStream.map { locationsOfInterest -> toMapLocationsOfInterest(locationsOfInterest) },
        selectedLocationOfInterest
      ) { locationsOfInterest, selectedLocationOfInterest ->
        updateSelectedLocationOfInterest(locationsOfInterest, selectedLocationOfInterest)
      }

    mapLocationsOfInterest =
      LiveDataReactiveStreams.fromPublisher(
        Flowable.combineLatest(
            listOf(
              savedMapLocationsOfInterest.startWith(ImmutableSet.of<MapLocationOfInterest>()),
            )
          ) { concatLocationsOfInterestSets(it) }
          .distinctUntilChanged()
      )

    mbtilesFilePaths =
      LiveDataReactiveStreams.fromPublisher(
        offlineAreaRepository.downloadedTileSetsOnceAndStream.map { set: ImmutableSet<TileSet> ->
          set.map(TileSet::path).toImmutableSet()
        }
      )
  }
}
