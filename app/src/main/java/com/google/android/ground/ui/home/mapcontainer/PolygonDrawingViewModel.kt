/*
 * Copyright 2021 Google LLC
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
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import com.google.android.ground.R
import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.map.MapLocationOfInterest
import com.google.auto.value.AutoValue
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java8.util.Optional
import javax.inject.Inject
import timber.log.Timber

@SharedViewModel
class PolygonDrawingViewModel
@Inject
internal constructor(
  private val locationManager: LocationManager,
  private val authManager: AuthenticationManager,
  private val uuidGenerator: OfflineUuidGenerator
) : AbstractViewModel() {
  private val polygonDrawingState: @Hot Subject<PolygonDrawingState> = PublishSubject.create()
  private val partialPolygonLocationOfInterestFlowable:
    @Hot
    Subject<Optional<MapLocationOfInterest>> =
    PublishSubject.create()

  /** Denotes whether the drawn polygon is complete or not. This is different from drawing state. */
  val isPolygonCompleted: @Hot LiveData<Boolean>

  /** Locations of interest drawn by the user but not yet saved. */
  val unsavedMapLocationsOfInterest: @Hot LiveData<ImmutableSet<MapLocationOfInterest>>

  private val locationLockEnabled: @Hot(replays = true) MutableLiveData<Boolean> = MutableLiveData()

  val iconTint: LiveData<Int>
  private val locationLockChangeRequests: @Hot Subject<Boolean> = PublishSubject.create()
  private val locationLockState: LiveData<Result<Boolean>>
  private val vertices: MutableList<Point> = ArrayList()

  /** The currently selected job and survey for the polygon drawing. */
  private val selectedJob = BehaviorProcessor.create<Job>()
  private val selectedSurvey = BehaviorProcessor.create<Survey>()
  private var cameraTarget: Point? = null

  /**
   * If true, then it means that the last vertex is added automatically and should be removed before
   * adding any permanent vertex. Used for rendering a line between last added point and current
   * camera target.
   */
  private var isLastVertexNotSelectedByUser = false

  private var polygonLocationOfInterest = Optional.empty<MapLocationOfInterest>()

  private fun createLocationLockStateFlowable(): Flowable<Result<Boolean>> =
    locationLockChangeRequests
      .switchMapSingle { enabled ->
        if (enabled) locationManager.enableLocationUpdates()
        else locationManager.disableLocationUpdates()
      }
      .toFlowable(BackpressureStrategy.LATEST)

  val drawingState: @Hot Observable<PolygonDrawingState>
    get() = polygonDrawingState

  fun onCameraMoved(newTarget: Point) {
    cameraTarget = newTarget
    if (locationLockState.value != null && isLocationLockEnabled()) {
      Timber.d("User dragged map. Disabling location lock")
      locationLockChangeRequests.onNext(false)
    }
  }

  /**
   * Adds another vertex at the given point if {@param distanceInPixels} is more than the configured
   * threshold. Otherwise, snaps to the first vertex.
   *
   * @param newTarget Position of the map camera.
   * @param distanceInPixels Distance between the last vertex and {@param newTarget}.
   */
  fun updateLastVertex(newTarget: Point, distanceInPixels: Double) {
    val isPolygonComplete = vertices.size > 2 && distanceInPixels <= DISTANCE_THRESHOLD_DP
    addVertex((if (isPolygonComplete) vertices[0] else newTarget), true)
  }

  /** Attempts to remove the last vertex of drawn polygon, if any. */
  fun removeLastVertex() {
    if (vertices.isEmpty()) {
      polygonDrawingState.onNext(PolygonDrawingState.canceled())
      reset()
    } else {
      vertices.removeAt(vertices.size - 1)
      updateVertices(ImmutableList.copyOf(vertices))
    }
  }

  fun selectCurrentVertex() = cameraTarget?.let { addVertex(it, false) }

  fun setLocationLockEnabled(enabled: Boolean) {
    locationLockEnabled.postValue(enabled)
  }

  /**
   * Adds a new vertex.
   *
   * @param vertex new position
   * @param isNotSelectedByUser whether the vertex is not selected by the user
   */
  private fun addVertex(vertex: Point, isNotSelectedByUser: Boolean) {
    // Clear last vertex if it is unselected
    if (isLastVertexNotSelectedByUser && vertices.isNotEmpty()) {
      vertices.removeAt(vertices.size - 1)
    }

    // Update selected state
    isLastVertexNotSelectedByUser = isNotSelectedByUser

    // Add the new vertex
    vertices.add(vertex)

    // Render changes to UI
    updateVertices(ImmutableList.copyOf(vertices))
  }

  private fun updateVertices(newVertices: ImmutableList<Point>) {
    polygonLocationOfInterest =
      polygonLocationOfInterest.map {
        it.copy(
          locationOfInterest =
            it.locationOfInterest.copy(
              geometry = Polygon(LinearRing(newVertices.map { point -> point.coordinate }))
            )
        )
      }
    partialPolygonLocationOfInterestFlowable.onNext(polygonLocationOfInterest)
  }

  fun onCompletePolygonButtonClick() {
    check(!(selectedJob.value == null || selectedSurvey.value == null)) { "Survey or job is null" }
    val locationOfInterest = polygonLocationOfInterest.get().locationOfInterest
    check(polygonLocationOfInterest.get().isPolygonComplete()) { "Polygon is not complete" }
    val auditInfo = AuditInfo(authManager.currentUser)
    val completedPolygon =
      Polygon(LinearRing(locationOfInterest.geometry.vertices.map { it.coordinate }))
    val areaOfInterest =
      LocationOfInterest(
        id = locationOfInterest.id,
        geometry = completedPolygon,
        surveyId = selectedSurvey.value!!.id,
        job = selectedJob.value!!,
        created = auditInfo,
        lastModified = auditInfo,
      )
    polygonDrawingState.onNext(PolygonDrawingState.completed(areaOfInterest))
    reset()
  }

  private fun reset() {
    isLastVertexNotSelectedByUser = false
    vertices.clear()
    polygonLocationOfInterest = Optional.empty()
    partialPolygonLocationOfInterestFlowable.onNext(Optional.empty())
  }

  val firstVertex: Optional<Point>
    get() = polygonLocationOfInterest.map { it.locationOfInterest.geometry.vertices[0] }

  fun onLocationLockClick() = locationLockChangeRequests.onNext(!isLocationLockEnabled())

  private fun isLocationLockEnabled(): Boolean = locationLockState.value!!.getOrDefault(false)

  // TODO : current location is not working value is always false.
  fun getLocationLockEnabled(): LiveData<Boolean> = locationLockEnabled

  fun startDrawingFlow(selectedSurvey: Survey, selectedJob: Job) {
    this.selectedJob.onNext(selectedJob)
    this.selectedSurvey.onNext(selectedSurvey)
    polygonDrawingState.onNext(PolygonDrawingState.inProgress())

    val auditInfo = AuditInfo(authManager.currentUser)
    polygonLocationOfInterest =
      Optional.of(
        MapLocationOfInterest(
          LocationOfInterest(
            id = uuidGenerator.generateUuid(),
            geometry = LineString(ImmutableList.of()),
            surveyId = selectedSurvey.id,
            job = selectedJob,
            created = auditInfo,
            lastModified = auditInfo,
          )
        )
      )
  }

  @AutoValue
  abstract class PolygonDrawingState {
    val isCanceled: Boolean
      get() = state == State.CANCELED
    val isInProgress: Boolean
      get() = state == State.IN_PROGRESS
    val isCompleted: Boolean
      get() = state == State.COMPLETED

    /** Represents state of PolygonDrawing action. */
    enum class State {
      IN_PROGRESS,
      COMPLETED,
      CANCELED
    }

    /** Current state of polygon drawing. */
    abstract val state: State

    /** Final polygon location of interest. */
    abstract val unsavedPolygonLocationOfInterest: LocationOfInterest?

    companion object {
      fun canceled(): PolygonDrawingState {
        return createDrawingState(State.CANCELED, null)
      }

      fun inProgress(): PolygonDrawingState {
        return createDrawingState(State.IN_PROGRESS, null)
      }

      fun completed(unsavedAreaOfInterest: LocationOfInterest?): PolygonDrawingState {
        return createDrawingState(State.COMPLETED, unsavedAreaOfInterest)
      }

      private fun createDrawingState(
        state: State,
        unsavedAreaOfInterest: LocationOfInterest?
      ): PolygonDrawingState {
        return AutoValue_PolygonDrawingViewModel_PolygonDrawingState(state, unsavedAreaOfInterest)
      }
    }
  }

  /**
   * Returns a set of [MapLocationOfInterest] to be drawn on map for the given
   * [MapLocationOfInterest].
   */
  private fun unsavedLocationsOfInterestFromLocationOfInterest(
    polygonLocationOfInterest: MapLocationOfInterest
  ): ImmutableSet<MapLocationOfInterest> {
    check(!(selectedJob.value == null || selectedSurvey.value == null)) { "Survey or job is null" }
    val locationOfInterest = polygonLocationOfInterest.locationOfInterest
    val vertices = locationOfInterest.geometry.vertices

    if (vertices.isEmpty()) {
      return ImmutableSet.of()
    }

    val auditInfo = AuditInfo(authManager.currentUser)
    // Include the given polygon and add 1 LOI with a Point for each of its vertex.
    return ImmutableSet.builder<MapLocationOfInterest>()
      .add(polygonLocationOfInterest)
      .addAll(
        vertices
          .map { point ->
            MapLocationOfInterest(
              LocationOfInterest(
                locationOfInterest.id,
                selectedSurvey.value!!.id,
                selectedJob.value!!,
                created = auditInfo,
                lastModified = auditInfo,
                geometry = point
              )
            )
          }
          .toList()
      )
      .build()
  }

  companion object {
    /** Min. distance in dp between two points for them be considered as overlapping. */
    const val DISTANCE_THRESHOLD_DP = 24
  }

  init {
    // TODO: Create custom ui component for location lock button and share across app.
    val locationLockStateFlowable = createLocationLockStateFlowable().share()
    locationLockState =
      LiveDataReactiveStreams.fromPublisher(
        locationLockStateFlowable.startWith(Result.success(false))
      )
    iconTint =
      LiveDataReactiveStreams.fromPublisher(
        locationLockStateFlowable
          .map { locked ->
            if (locked.getOrDefault(false)) R.color.colorMapBlue else R.color.colorGrey800
          }
          .startWith(R.color.colorGrey800)
      )
    val polygonFlowable =
      partialPolygonLocationOfInterestFlowable
        .startWith(Optional.empty())
        .toFlowable(BackpressureStrategy.LATEST)
        .share()
    isPolygonCompleted =
      LiveDataReactiveStreams.fromPublisher(
        polygonFlowable
          .map { polygon -> polygon.map { it.isPolygonComplete() }.orElse(false) }
          .startWith(false)
      )
    unsavedMapLocationsOfInterest =
      LiveDataReactiveStreams.fromPublisher(
        polygonFlowable.map { polygon ->
          polygon
            .map { unsavedLocationsOfInterestFromLocationOfInterest(it) }
            .orElse(ImmutableSet.of())
        }
      )
  }

  private fun MapLocationOfInterest.isPolygonComplete(): Boolean {
    val vertices = this.locationOfInterest.geometry.vertices
    if (vertices.size < 4) {
      return false
    }
    val first: Point = vertices[0]
    val last: Point = vertices[vertices.lastIndex]
    return first == last
  }
}
