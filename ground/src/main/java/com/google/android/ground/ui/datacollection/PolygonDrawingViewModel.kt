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
package com.google.android.ground.ui.datacollection

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.editsubmission.AbstractTaskViewModel
import com.google.android.ground.ui.map.Feature
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java8.util.Optional
import javax.inject.Inject

@SharedViewModel
class PolygonDrawingViewModel
@Inject
internal constructor(private val uuidGenerator: OfflineUuidGenerator, resources: Resources) :
  AbstractTaskViewModel(resources) {
  private val polygonDrawingState: @Hot Subject<PolygonDrawingState> = PublishSubject.create()
  private val partialPolygonFlowable: @Hot Subject<Optional<Polygon>> = PublishSubject.create()

  /** Denotes whether the drawn polygon is complete or not. This is different from drawing state. */
  val isPolygonCompleted: @Hot LiveData<Boolean>

  /** [Feature]s drawn by the user but not yet saved. */
  val features: @Hot LiveData<ImmutableSet<Feature>>

  private val vertices: MutableList<Point> = ArrayList()
  private var polygon: Polygon? = null
  private var cameraTarget: Point? = null

  /**
   * If true, then it means that the last vertex is added automatically and should be removed before
   * adding any permanent vertex. Used for rendering a line between last added point and current
   * camera target.
   */
  private var isLastVertexNotSelectedByUser = false

  val drawingState: @Hot Observable<PolygonDrawingState>
    get() = polygonDrawingState

  fun onCameraMoved(newTarget: Point) {
    cameraTarget = newTarget
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
    val polygon = Polygon(LinearRing(newVertices.map { point -> point.coordinate }))
    partialPolygonFlowable.onNext(Optional.of(polygon))
    this.polygon = polygon
  }

  fun onCompletePolygonButtonClick() {
    check(polygon!!.isPolygonComplete()) { "Polygon is not complete" }
    polygonDrawingState.onNext(PolygonDrawingState.completed(polygon!!))
    reset()
  }

  private fun reset() {
    isLastVertexNotSelectedByUser = false
    vertices.clear()
    polygon = null
    partialPolygonFlowable.onNext(Optional.empty())
  }

  val firstVertex: Optional<Point>
    get() = Optional.ofNullable(vertices.firstOrNull())

  fun startDrawingFlow() {
    polygonDrawingState.onNext(PolygonDrawingState.inProgress())
  }

  data class PolygonDrawingState(val state: State, val polygon: Polygon? = null) {
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

    companion object {
      fun canceled(): PolygonDrawingState = PolygonDrawingState(State.CANCELED)

      fun inProgress(): PolygonDrawingState = PolygonDrawingState(State.IN_PROGRESS)

      fun completed(polygon: Polygon): PolygonDrawingState =
        PolygonDrawingState(State.COMPLETED, polygon)
    }
  }

  /** Returns a set of [Feature] to be drawn on map for the given [Polygon]. */
  private fun createFeatures(polygon: Polygon): ImmutableSet<Feature> {
    val vertices = polygon.vertices

    if (vertices.isEmpty()) {
      return ImmutableSet.of()
    }

    return ImmutableSet.builder<Feature>()
      .add(
        Feature(
          id = uuidGenerator.generateUuid(),
          tag = Feature.Type.LOCATION_OF_INTEREST,
          geometry = polygon
        )
      )
      .build()
  }

  companion object {
    /** Min. distance in dp between two points for them be considered as overlapping. */
    const val DISTANCE_THRESHOLD_DP = 24
  }

  init {
    val polygonFlowable =
      partialPolygonFlowable
        .startWith(Optional.empty())
        .toFlowable(BackpressureStrategy.LATEST)
        .share()
    isPolygonCompleted =
      LiveDataReactiveStreams.fromPublisher(
        polygonFlowable
          .map { polygon -> polygon.map { it.isPolygonComplete() }.orElse(false) }
          .startWith(false)
      )
    features =
      LiveDataReactiveStreams.fromPublisher(
        polygonFlowable.map { polygon ->
          polygon.map { createFeatures(it) }.orElse(ImmutableSet.of())
        }
      )
  }

  private fun Polygon.isPolygonComplete(): Boolean {
    if (vertices.size < 4) {
      return false
    }
    val first: Point = vertices[0]
    val last: Point = vertices[vertices.lastIndex]
    return first == last
  }
}
