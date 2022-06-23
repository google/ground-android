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
package com.google.android.gnd.ui.home.mapcontainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import com.google.android.gnd.R
import com.google.android.gnd.model.AuditInfo
import com.google.android.gnd.model.Survey
import com.google.android.gnd.model.feature.Point
import com.google.android.gnd.model.feature.PolygonFeature
import com.google.android.gnd.model.job.Job
import com.google.android.gnd.model.job.Style
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator
import com.google.android.gnd.rx.BooleanOrError
import com.google.android.gnd.rx.BooleanOrError.Companion.falseValue
import com.google.android.gnd.rx.annotations.Hot
import com.google.android.gnd.system.LocationManager
import com.google.android.gnd.system.auth.AuthenticationManager
import com.google.android.gnd.ui.common.AbstractViewModel
import com.google.android.gnd.ui.common.SharedViewModel
import com.google.android.gnd.ui.map.MapFeature
import com.google.android.gnd.ui.map.MapPin
import com.google.android.gnd.ui.map.MapPolygon
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
import timber.log.Timber
import javax.inject.Inject

@SharedViewModel
class PolygonDrawingViewModel @Inject internal constructor(
    private val locationManager: LocationManager,
    private val authManager: AuthenticationManager,
    private val uuidGenerator: OfflineUuidGenerator
) : AbstractViewModel() {
    private val polygonDrawingState: @Hot Subject<PolygonDrawingState> = PublishSubject.create()
    private val mapPolygonFlowable: @Hot Subject<Optional<MapPolygon>> = PublishSubject.create()

    /** Denotes whether the drawn polygon is complete or not. This is different from drawing state.  */
    val isPolygonCompleted: @Hot LiveData<Boolean>

    /** Features drawn by the user but not yet saved.  */
    val unsavedMapFeatures: @Hot LiveData<ImmutableSet<MapFeature>>

    private val locationLockEnabled: @Hot(replays = true) MutableLiveData<Boolean> =
        MutableLiveData()

    val iconTint: LiveData<Int>
    private val locationLockChangeRequests: @Hot Subject<Boolean> = PublishSubject.create()
    private val locationLockState: LiveData<BooleanOrError>
    private val vertices: MutableList<Point> = ArrayList()

    /** The currently selected job and survey for the polygon drawing.  */
    private val selectedJob = BehaviorProcessor.create<Job>()
    private val selectedSurvey = BehaviorProcessor.create<Survey>()
    private var cameraTarget: Point? = null

    /**
     * If true, then it means that the last vertex is added automatically and should be removed before
     * adding any permanent vertex. Used for rendering a line between last added point and current
     * camera target.
     */
    private var isLastVertexNotSelectedByUser = false

    private var mapPolygon = Optional.empty<MapPolygon>()

    private fun createLocationLockStateFlowable(): Flowable<BooleanOrError> =
        locationLockChangeRequests
            .switchMapSingle { enabled -> if (enabled) locationManager.enableLocationUpdates() else locationManager.disableLocationUpdates() }
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

    /** Attempts to remove the last vertex of drawn polygon, if any.  */
    fun removeLastVertex() {
        if (vertices.isEmpty()) {
            polygonDrawingState.onNext(PolygonDrawingState.canceled())
            reset()
        } else {
            vertices.removeAt(vertices.size - 1)
            updateVertices(ImmutableList.copyOf(vertices))
        }
    }

    fun selectCurrentVertex() =
        cameraTarget?.let {
            addVertex(it, false)
        }

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
        mapPolygon = mapPolygon.map { polygon: MapPolygon ->
            polygon.toBuilder().setVertices(newVertices).build()
        }
        mapPolygonFlowable.onNext(mapPolygon)
    }

    fun onCompletePolygonButtonClick() {
        check(!(selectedJob.value == null || selectedSurvey.value == null)) { "Survey or job is null" }
        val polygon = mapPolygon.get()
        check(polygon.isPolygonComplete) { "Polygon is not complete" }
        val auditInfo = AuditInfo.now(authManager.currentUser)
        val polygonFeature = PolygonFeature.builder()
            .setId(polygon.id)
            .setVertices(polygon.vertices)
            .setSurvey(selectedSurvey.value!!)
            .setJob(selectedJob.value!!)
            .setCreated(auditInfo)
            .setLastModified(auditInfo)
            .build()
        polygonDrawingState.onNext(PolygonDrawingState.completed(polygonFeature))
        reset()
    }

    private fun reset() {
        isLastVertexNotSelectedByUser = false
        vertices.clear()
        mapPolygon = Optional.empty()
        mapPolygonFlowable.onNext(Optional.empty())
    }

    val firstVertex: Optional<Point>
        get() = mapPolygon.map { it.firstVertex }

    fun onLocationLockClick() =
        locationLockChangeRequests.onNext(!isLocationLockEnabled())

    private fun isLocationLockEnabled(): Boolean = locationLockState.value!!.isTrue

    // TODO : current location is not working value is always false.
    fun getLocationLockEnabled(): LiveData<Boolean> = locationLockEnabled

    fun startDrawingFlow(selectedSurvey: Survey, selectedJob: Job) {
        this.selectedJob.onNext(selectedJob)
        this.selectedSurvey.onNext(selectedSurvey)
        polygonDrawingState.onNext(PolygonDrawingState.inProgress())

        mapPolygon = Optional.of(
            MapPolygon.newBuilder()
                .setId(uuidGenerator.generateUuid())
                .setVertices(ImmutableList.of())
                .setStyle(Style.DEFAULT_MAP_STYLE)
                .build()
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

        /** Represents state of PolygonDrawing action.  */
        enum class State {
            IN_PROGRESS, COMPLETED, CANCELED
        }

        /** Current state of polygon drawing.  */
        abstract val state: State

        /** Final polygon feature.  */
        abstract val unsavedPolygonFeature: PolygonFeature?

        companion object {
            fun canceled(): PolygonDrawingState {
                return createDrawingState(State.CANCELED, null)
            }

            fun inProgress(): PolygonDrawingState {
                return createDrawingState(State.IN_PROGRESS, null)
            }

            fun completed(unsavedFeature: PolygonFeature?): PolygonDrawingState {
                return createDrawingState(State.COMPLETED, unsavedFeature)
            }

            private fun createDrawingState(
                state: State, unsavedFeature: PolygonFeature?
            ): PolygonDrawingState {
                return AutoValue_PolygonDrawingViewModel_PolygonDrawingState(state, unsavedFeature)
            }
        }
    }

    companion object {
        /** Min. distance in dp between two points for them be considered as overlapping.  */
        const val DISTANCE_THRESHOLD_DP = 24

        /** Returns a set of [MapFeature] to be drawn on map for the given [MapPolygon].  */
        private fun unsavedFeaturesFromPolygon(mapPolygon: MapPolygon): ImmutableSet<MapFeature> {
            val vertices = mapPolygon.vertices

            if (vertices.isEmpty()) {
                return ImmutableSet.of()
            }

            // Include the given polygon and add 1 MapPin for each of its vertex.
            return ImmutableSet.builder<MapFeature>()
                .add(mapPolygon)
                .addAll(
                    vertices
                        .map { point ->
                            MapPin.newBuilder()
                                .setId(mapPolygon.id)
                                .setPosition(point)
                                // TODO: Use different marker style for unsaved markers.
                                .setStyle(mapPolygon.style)
                                .build()
                        }
                        .toList())
                .build()
        }
    }

    init {
        // TODO: Create custom ui component for location lock button and share across app.
        val locationLockStateFlowable = createLocationLockStateFlowable().share()
        locationLockState = LiveDataReactiveStreams.fromPublisher(
            locationLockStateFlowable.startWith(falseValue())
        )
        iconTint = LiveDataReactiveStreams.fromPublisher(
            locationLockStateFlowable
                .map { locked -> if (locked.isTrue) R.color.colorMapBlue else R.color.colorGrey800 }
                .startWith(R.color.colorGrey800))
        val polygonFlowable = mapPolygonFlowable
            .startWith(Optional.empty())
            .toFlowable(BackpressureStrategy.LATEST)
            .share()
        isPolygonCompleted = LiveDataReactiveStreams.fromPublisher(
            polygonFlowable
                .map { polygon ->
                    polygon.map { it.isPolygonComplete }
                        .orElse(false)
                }
                .startWith(false))
        unsavedMapFeatures = LiveDataReactiveStreams.fromPublisher(
            polygonFlowable.map { polygon ->
                polygon
                    .map { unsavedFeaturesFromPolygon(it) }
                    .orElse(ImmutableSet.of())
            })
    }
}