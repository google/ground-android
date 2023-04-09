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
package com.google.android.ground.ui.datacollection.tasks.polygon

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.viewModelScope
import com.google.android.ground.model.geometry.*
import com.google.android.ground.model.geometry.GeometryValidator.Companion.isComplete
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.FeatureType
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@SharedViewModel
class PolygonDrawingViewModel
@Inject
internal constructor(private val uuidGenerator: OfflineUuidGenerator, resources: Resources) :
  AbstractTaskViewModel(resources) {
  private val partialPolygonFlowable: @Hot Subject<Polygon> = PublishSubject.create()

  private val verticesFlow: MutableStateFlow<List<Point>> = MutableStateFlow(listOf())
  val verticesValue: StateFlow<List<Point>> =
    verticesFlow.stateIn(viewModelScope, SharingStarted.Lazily, listOf())

  /** [Feature]s drawn by the user but not yet saved. */
  private val featureFlow: MutableStateFlow<Feature?> = MutableStateFlow(null)
  val featureValue: StateFlow<Feature?> =
    featureFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

  val polygonLiveData: @Hot LiveData<Polygon>

  //  val features: @Hot LiveData<Set<Feature>>

  /** Represents the current state of polygon that is being drawn. */
  //  private var polygon: Polygon = Polygon.EMPTY_POLYGON
  private var vertices: List<Point> = listOf()

  /** Represents whether the user has completed drawing the polygon or not. */
  private var isMarkedComplete: Boolean = false

  init {
    val polygonFlowable = partialPolygonFlowable.toFlowable(BackpressureStrategy.LATEST).share()
    polygonLiveData = LiveDataReactiveStreams.fromPublisher(polygonFlowable)
    //    features = LiveDataReactiveStreams.fromPublisher(polygonFlowable.map {
    //      createFeatures(it) })
  }

  /**
   * Overwrites last vertex at the given point if the distance is pixels between first vertex and
   * new target is more than the configured threshold. Otherwise, snaps to the first vertex.
   */
  fun updateLastVertexAndMaybeCompletePolygon(
    target: Coordinate,
    calculateDistanceInPixels: (c1: Coordinate, c2: Coordinate) -> Double
  ) {
    if (isMarkedComplete) return

    val firstVertex = vertices.firstOrNull()
    var updatedTarget = target
    if (firstVertex != null && vertices.size > 2) {
      val distance = calculateDistanceInPixels(firstVertex.coordinate, target)
      if (distance <= DISTANCE_THRESHOLD_DP) {
        updatedTarget = firstVertex.coordinate
      }
    }

    addVertex(updatedTarget, true)
  }

  override fun clearResponse() {
    removeLastVertex()
  }

  /** Attempts to remove the last vertex of drawn polygon, if any. */
  fun removeLastVertex() {
    // Do nothing if there are no vertices to remove.
    if (vertices.isEmpty()) return

    // Reset complete status
    isMarkedComplete = false

    // Remove last vertex and update polygon
    val updatedVertices = vertices.toMutableList()
    updatedVertices.removeLast()
    updateVertices(updatedVertices.toImmutableList())
  }

  /** Adds the last vertex to the polygon. */
  fun addLastVertex() = vertices.lastOrNull()?.let { addVertex(it.coordinate, false) }

  /**
   * Adds a new vertex to the polygon.
   *
   * @param vertex
   * @param shouldOverwriteLastVertex
   */
  private fun addVertex(vertex: Coordinate, shouldOverwriteLastVertex: Boolean) {
    val updatedVertices = vertices.toMutableList()

    // Maybe remove the last vertex before adding the new vertex.
    if (shouldOverwriteLastVertex && updatedVertices.isNotEmpty()) {
      updatedVertices.removeLast()
    }

    // Add the new vertex
    updatedVertices.add(Point(vertex))

    // Render changes to UI
    updateVertices(updatedVertices.toImmutableList())
  }

  private fun updateVertices(newVertices: List<Point>) {
    //    val polygon = Polygon(LinearRing(newVertices.map { point -> point.coordinate }))
    //    partialPolygonFlowable.onNext(polygon)
    //    this.polygon = polygon
    verticesFlow.value = newVertices
    this.vertices = newVertices
    refreshFeatures(newVertices)
  }

  fun onCompletePolygonButtonClick() {
    //    check(polygon.isComplete()) { "Polygon is not complete" }
    isMarkedComplete = true
    //    partialPolygonFlowable.onNext(polygon)
    // TODO: Serialize the polygon and update response
  }

  /** Returns a set of [Feature] to be drawn on map for the given [Polygon]. */
  private fun refreshFeatures(points: List<Point>) {
    if (points.isEmpty()) {
      featureFlow.value = null
      return
    }

    featureFlow.value =
      Feature(
        id = uuidGenerator.generateUuid(),
        type = FeatureType.USER_POLYGON.ordinal,
        geometry = createGeometry(points)
      )
  }

  /** Returns a map geometry to be drawn based on given list of points. */
  private fun createGeometry(points: List<Point>): Geometry {
    val coordinates = points.map { it.coordinate }
    if (isMarkedComplete) {
      return Polygon(LinearRing(coordinates))
    }
    if (coordinates.isComplete()) {
      return LinearRing(coordinates)
    }
    return LineString(coordinates)
  }

  companion object {
    /** Min. distance in dp between two points for them be considered as overlapping. */
    const val DISTANCE_THRESHOLD_DP = 24
  }
}
