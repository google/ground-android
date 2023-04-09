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
import androidx.lifecycle.viewModelScope
import com.google.android.ground.model.geometry.*
import com.google.android.ground.model.geometry.GeometryValidator.Companion.isComplete
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.FeatureType
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

  /** [Feature]s drawn by the user but not yet saved. */
  private val featureFlow: MutableStateFlow<Feature?> = MutableStateFlow(null)
  val featureValue: StateFlow<Feature?> =
    featureFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

  /** Represents the current state of polygon that is being drawn. */
  private var vertices: List<Point> = listOf()

  /** Represents whether the user has completed drawing the polygon or not. */
  private var isMarkedComplete: Boolean = false

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
    this.vertices = newVertices
    refreshFeatures(newVertices, false)
  }

  fun onCompletePolygonButtonClick() {
    isMarkedComplete = true

    refreshFeatures(vertices, true)
    // TODO: Serialize the polygon and update response
  }

  /** Returns a set of [Feature] to be drawn on map for the given [Polygon]. */
  private fun refreshFeatures(points: List<Point>, isMarkedComplete: Boolean) {
    if (points.isEmpty()) {
      featureFlow.value = null
      return
    }

    featureFlow.value =
      Feature(
        id = uuidGenerator.generateUuid(),
        type = FeatureType.USER_POLYGON.ordinal,
        geometry = createGeometry(points.map { it.coordinate }, isMarkedComplete)
      )
  }

  /** Returns a map geometry to be drawn based on given list of points. */
  private fun createGeometry(coordinates: List<Coordinate>, isMarkedComplete: Boolean): Geometry =
    if (isMarkedComplete && coordinates.isComplete()) {
      Polygon(LinearRing(coordinates))
    } else if (coordinates.isComplete()) {
      LinearRing(coordinates)
    } else {
      LineString(coordinates)
    }

  companion object {
    /** Min. distance in dp between two points for them be considered as overlapping. */
    const val DISTANCE_THRESHOLD_DP = 24
  }
}
