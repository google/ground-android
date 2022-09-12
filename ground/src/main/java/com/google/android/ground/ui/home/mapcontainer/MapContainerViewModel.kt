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
import android.location.Location
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.Dimension
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.google.android.ground.R
import com.google.android.ground.model.Survey
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.job.Style
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.locationofinterest.LocationOfInterestType
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.BooleanOrError
import com.google.android.ground.rx.BooleanOrError.Companion.falseValue
import com.google.android.ground.rx.Event
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.Loadable.Companion.getValue
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.LocationManager
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.map.*
import com.google.android.ground.util.toImmutableSet
import com.google.common.collect.ImmutableSet
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*
import java8.util.Optional
import javax.inject.Inject
import timber.log.Timber

@SharedViewModel
class MapContainerViewModel
@Inject
internal constructor(
  private val resources: Resources,
  private val surveyRepository: SurveyRepository,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val locationManager: LocationManager,
  offlineAreaRepository: OfflineAreaRepository
) : AbstractViewModel() {
  val surveyLoadingState: LiveData<Loadable<Survey>>
  val mapLocationsOfInterest: LiveData<ImmutableSet<MapLocationOfInterest>>
  val locationLockState: LiveData<BooleanOrError>
  val cameraUpdateRequests: LiveData<Event<CameraUpdate>>

  private val cameraPosition: @Hot(replays = true) MutableLiveData<CameraPosition> =
    MutableLiveData(CameraPosition(DEFAULT_MAP_POINT, DEFAULT_MAP_ZOOM_LEVEL))

  private val locationLockChangeRequests: @Hot Subject<Boolean> = PublishSubject.create()
  private val cameraUpdateSubject: @Hot Subject<CameraUpdate> = PublishSubject.create()

  /** Temporary set of [MapLocationOfInterest] used for displaying on map during add/edit flows. */
  private val unsavedMapLocationsOfInterest:
    @Hot
    PublishProcessor<ImmutableSet<MapLocationOfInterest>> =
    PublishProcessor.create()

  private val mapControlsVisibility: @Hot(replays = true) MutableLiveData<Int> =
    MutableLiveData(View.VISIBLE)

  private val addPolygonVisibility = MutableLiveData(View.GONE)

  private val moveLocationsOfInterestVisibility: @Hot(replays = true) MutableLiveData<Int> =
    MutableLiveData(View.GONE)

  private val locationLockEnabled: @Hot(replays = true) MutableLiveData<Boolean> = MutableLiveData()

  private val locationOfInterestAddButtonBackgroundTint: @Hot(replays = true) MutableLiveData<Int> =
    MutableLiveData(R.color.colorGrey500)

  val mbtilesFilePaths: LiveData<ImmutableSet<String>>
  val iconTint: LiveData<Int>
  val isLocationUpdatesEnabled: LiveData<Boolean>
  val locationAccuracy: LiveData<String>
  private val tileProviders: MutableList<MapBoxOfflineTileProvider> = ArrayList()

  @Dimension
  private val defaultPolygonStrokeWidth: Int =
    resources.getDimension(R.dimen.polyline_stroke_width).toInt()

  @Dimension
  private val selectedPolygonStrokeWidth: Int =
    resources.getDimension(R.dimen.selected_polyline_stroke_width).toInt()

  /** The currently selected LOI on the map. */
  private val selectedLocationOfInterest =
    BehaviorProcessor.createDefault(Optional.empty<LocationOfInterest>())

  /* UI Clicks */
  private val selectMapTypeClicks: @Hot Subject<Nil> = PublishSubject.create()
  private val zoomThresholdCrossed: @Hot Subject<Nil> = PublishSubject.create()

  // TODO: Move this in LocationOfInterestRepositionView and return the final updated LOI as the
  // result.
  /** LocationOfInterest selected for repositioning. */
  var reposLocationOfInterest: Optional<LocationOfInterest> = Optional.empty()

  private fun onSurveyChange(project: Optional<Survey>) =
    project
      .map(Survey::id)
      .flatMap { surveyId: String -> surveyRepository.getLastCameraPosition(surveyId) }
      .ifPresent { cameraPosition: CameraPosition -> this.panAndZoomCamera(cameraPosition) }

  fun setUnsavedMapLocationsOfInterest(locationsOfInterest: ImmutableSet<MapLocationOfInterest>) =
    unsavedMapLocationsOfInterest.onNext(locationsOfInterest)

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
    val mapPins =
      locationsOfInterest
        .filter { it.type === LocationOfInterestType.POINT }
        .map { toMapPin(it) }
        .toImmutableSet()

    // TODO: Add support for polylines similar to mapPins.
    val mapPolygons =
      locationsOfInterest
        .filter { it.type === LocationOfInterestType.POLYGON }
        .map { toMapPolygon(it) }
        .toImmutableSet()

    return ImmutableSet.builder<MapLocationOfInterest>().addAll(mapPins).addAll(mapPolygons).build()
  }

  private fun createLocationAccuracyFlowable(lockState: Flowable<BooleanOrError>) =
    lockState.switchMap { booleanOrError ->
      if (booleanOrError.isTrue)
        locationManager.getLocationUpdates().map {
          resources.getString(R.string.location_accuracy, it.accuracy)
        }
      else Flowable.empty()
    }

  private fun createCameraUpdateFlowable(
    locationLockStateFlowable: Flowable<BooleanOrError>
  ): Flowable<Event<CameraUpdate>> =
    cameraUpdateSubject
      .toFlowable(BackpressureStrategy.LATEST)
      .mergeWith(
        locationLockStateFlowable.switchMap { lockState: BooleanOrError ->
          createLocationLockCameraUpdateFlowable(lockState)
        }
      )
      .map { Event.create(it) }

  private fun createLocationLockCameraUpdateFlowable(
    lockState: BooleanOrError
  ): Flowable<CameraUpdate> {
    if (!lockState.isTrue) {
      return Flowable.empty()
    }
    // The first update pans and zooms the camera to the appropriate zoom level; subsequent ones
    // only pan the map.
    val locationUpdates = locationManager.getLocationUpdates().map { it.toPoint() }
    return locationUpdates
      .take(1)
      .map { CameraUpdate.panAndZoomIn(it) }
      .concatWith(locationUpdates.map { CameraUpdate.pan(it) }.skip(1))
  }

  private fun createLocationLockStateFlowable(): Flowable<BooleanOrError> =
    locationLockChangeRequests
      .switchMapSingle { enabled ->
        if (enabled) locationManager.enableLocationUpdates()
        else locationManager.disableLocationUpdates()
      }
      .toFlowable(BackpressureStrategy.LATEST)

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

  fun getCameraPosition(): LiveData<CameraPosition> {
    Timber.d("Current position is ${cameraPosition.value}")
    return cameraPosition
  }

  private fun isLocationLockEnabled(): Boolean = locationLockState.value!!.isTrue

  fun onCameraMove(newCameraPosition: CameraPosition) {
    Timber.d("Setting position to $newCameraPosition")
    onZoomChange(cameraPosition.value!!.zoomLevel, newCameraPosition.zoomLevel)
    cameraPosition.value = newCameraPosition
    surveyLoadingState.value?.value()?.ifPresent {
      surveyRepository.setCameraPosition(it.id, newCameraPosition)
    }
  }

  private fun onZoomChange(oldZoomLevel: Float, newZoomLevel: Float) {
    val zoomThresholdCrossed =
      (oldZoomLevel < ZOOM_LEVEL_THRESHOLD && newZoomLevel >= ZOOM_LEVEL_THRESHOLD ||
        oldZoomLevel >= ZOOM_LEVEL_THRESHOLD && newZoomLevel < ZOOM_LEVEL_THRESHOLD)
    if (zoomThresholdCrossed) {
      this.zoomThresholdCrossed.onNext(Nil.NIL)
    }
  }

  fun onMapDrag() {
    if (isLocationLockEnabled()) {
      Timber.d("User dragged map. Disabling location lock")
      locationLockChangeRequests.onNext(false)
    }
  }

  fun onMarkerClick(mapLocationOfInterest: MapLocationOfInterest) {
    val locationOfInterest = mapLocationOfInterest.locationOfInterest
    if (locationOfInterest != null) {
      val geometry = locationOfInterest.geometry
      if (geometry is Point) {
        panAndZoomCamera(geometry)
      }
    }
  }

  private fun panAndZoomCamera(cameraPosition: CameraPosition) {
    cameraUpdateSubject.onNext(CameraUpdate.panAndZoom(cameraPosition))
  }

  fun panAndZoomCamera(position: Point) {
    cameraUpdateSubject.onNext(CameraUpdate.panAndZoomIn(position))
  }

  fun onLocationLockClick() {
    locationLockChangeRequests.onNext(!isLocationLockEnabled())
  }

  // TODO(#691): Create our own wrapper/interface for MbTiles providers.
  fun queueTileProvider(tileProvider: MapBoxOfflineTileProvider) {
    tileProviders.add(tileProvider)
  }

  fun closeProviders() {
    tileProviders.forEach { it.close() }
  }

  fun setMode(viewMode: Mode) {
    mapControlsVisibility.postValue(if (viewMode == Mode.DEFAULT) View.VISIBLE else View.GONE)
    moveLocationsOfInterestVisibility.postValue(
      if (viewMode == Mode.MOVE_POINT) View.VISIBLE else View.GONE
    )
    addPolygonVisibility.postValue(if (viewMode == Mode.DRAW_POLYGON) View.VISIBLE else View.GONE)

    if (viewMode == Mode.DEFAULT) {
      reposLocationOfInterest = Optional.empty()
    }
  }

  fun onMapTypeButtonClicked() {
    selectMapTypeClicks.onNext(Nil.NIL)
  }

  fun getSelectMapTypeClicks(): Observable<Nil> {
    return selectMapTypeClicks
  }

  fun getZoomThresholdCrossed(): Observable<Nil> {
    return zoomThresholdCrossed
  }

  fun getMapControlsVisibility(): LiveData<Int> {
    return mapControlsVisibility
  }

  val moveLocationOfInterestVisibility: LiveData<Int>
    get() = moveLocationsOfInterestVisibility

  fun getAddPolygonVisibility(): LiveData<Int> = addPolygonVisibility

  /** Called when a LOI is (de)selected. */
  fun setSelectedLocationOfInterest(selectedLocationOfInterest: Optional<LocationOfInterest>) {
    this.selectedLocationOfInterest.onNext(selectedLocationOfInterest)
  }

  fun setLocationOfInterestButtonBackgroundTint(@ColorRes colorRes: Int) {
    locationOfInterestAddButtonBackgroundTint.postValue(colorRes)
  }

  fun getLocationLockEnabled(): LiveData<Boolean> = locationLockEnabled

  fun setLocationLockEnabled(enabled: Boolean) {
    locationLockEnabled.postValue(enabled)
  }

  enum class Mode {
    DEFAULT,
    DRAW_POLYGON,
    MOVE_POINT
  }

  class CameraUpdate(
    val center: Point,
    val zoomLevel: Optional<Float>,
    val isAllowZoomOut: Boolean
  ) {
    override fun toString(): String =
      if (zoomLevel.isPresent) {
        "Pan + zoom"
      } else {
        "Pan"
      }

    companion object {
      fun pan(center: Point): CameraUpdate = CameraUpdate(center, Optional.empty(), false)

      fun panAndZoomIn(center: Point): CameraUpdate =
        CameraUpdate(center, Optional.of(DEFAULT_LOI_ZOOM_LEVEL), false)

      fun panAndZoom(cameraPosition: CameraPosition): CameraUpdate =
        CameraUpdate(cameraPosition.target, Optional.of(cameraPosition.zoomLevel), true)
    }
  }

  companion object {
    // Higher zoom levels means the map is more zoomed in. 0.0f is fully zoomed out.
    const val ZOOM_LEVEL_THRESHOLD = 16f
    const val DEFAULT_LOI_ZOOM_LEVEL = 18.0f
    private const val DEFAULT_MAP_ZOOM_LEVEL = 0.0f
    private val DEFAULT_MAP_POINT = Point(Coordinate(0.0, 0.0))

    private fun concatLocationsOfInterestSets(
      objects: Array<Any>
    ): ImmutableSet<MapLocationOfInterest> {
      return listOf(*objects).flatMap { it as ImmutableSet<MapLocationOfInterest> }.toImmutableSet()
    }

    private fun toMapPin(pointOfInterest: LocationOfInterest): MapLocationOfInterest =
      MapPin.newBuilder()
        .setId(pointOfInterest.id)
        .setPosition(pointOfInterest.geometry.vertices[0])
        .setStyle(Style())
        .setLocationOfInterest(pointOfInterest)
        .build()

    private fun toMapPolygon(areaOfInterest: LocationOfInterest): MapLocationOfInterest =
      MapPolygon.newBuilder()
        .setId(areaOfInterest.id)
        .setVertices(areaOfInterest.geometry.vertices)
        .setStyle(Style())
        .setLocationOfInterest(areaOfInterest)
        .build()

    private fun Location.toPoint(): Point = Point(Coordinate(latitude, longitude))
  }

  init {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE
    val locationLockStateFlowable = createLocationLockStateFlowable().share()
    locationLockState =
      LiveDataReactiveStreams.fromPublisher(locationLockStateFlowable.startWith(falseValue()))
    iconTint =
      LiveDataReactiveStreams.fromPublisher(
        locationLockStateFlowable
          .map { locked: BooleanOrError ->
            if (locked.isTrue) R.color.colorMapBlue else R.color.colorGrey800
          }
          .startWith(R.color.colorGrey800)
      )
    isLocationUpdatesEnabled =
      LiveDataReactiveStreams.fromPublisher(
        locationLockStateFlowable.map(BooleanOrError::isTrue).startWith(false)
      )
    locationAccuracy =
      LiveDataReactiveStreams.fromPublisher(
        createLocationAccuracyFlowable(locationLockStateFlowable)
      )
    cameraUpdateRequests =
      LiveDataReactiveStreams.fromPublisher(createCameraUpdateFlowable(locationLockStateFlowable))
    surveyLoadingState =
      LiveDataReactiveStreams.fromPublisher<Loadable<Survey>>(surveyRepository.surveyLoadingState)
    // TODO: Clear location of interest markers when survey is deactivated.
    // TODO: Since we depend on survey stream from repo anyway, this transformation can be moved
    // into the repo
    // LOIs that are persisted to the local and remote dbs.
    val savedMapLocationsOfInterest =
      Flowable.combineLatest(
        surveyRepository.activeSurvey
          .switchMap { activeProject -> getLocationsOfInterestStream(activeProject) }
          .map { locationsOfInterest -> toMapLocationsOfInterest(locationsOfInterest) },
        selectedLocationOfInterest
      ) { locationsOfInterest, selectedLocationOfInterest ->
        updateSelectedLocationOfInterest(locationsOfInterest, selectedLocationOfInterest)
      }

    mapLocationsOfInterest =
      LiveDataReactiveStreams.fromPublisher(
        Flowable.combineLatest(
            listOf(
              savedMapLocationsOfInterest.startWith(ImmutableSet.of<MapLocationOfInterest>()),
              unsavedMapLocationsOfInterest.startWith(ImmutableSet.of<MapLocationOfInterest>())
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
    disposeOnClear(
      surveyRepository.activeSurvey.subscribe { project: Optional<Survey> ->
        onSurveyChange(project)
      }
    )
  }
}
