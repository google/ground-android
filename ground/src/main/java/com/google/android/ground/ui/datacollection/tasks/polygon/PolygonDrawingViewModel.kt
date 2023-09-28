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
import android.graphics.Color
import androidx.lifecycle.viewModelScope
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.GeometryValidator.isComplete
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.submission.GeometryData
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

  /**
   * [Feature]s drawn by the user, but not yet saved.
   *
   * Can be one of LineString, LinearRing, or Polygon.
   */
  private val featureFlow: MutableStateFlow<Feature?> = MutableStateFlow(null)
  val featureValue: StateFlow<Feature?> =
    featureFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

  /**
   * List of [Point]s to be used for generating the [Geometry]. If [isMarkedComplete] is false, then
   * the last vertex represents the map center and the second last vertex is the last added vertex.
   */
  private var vertices: List<Point> = listOf()

  /** Represents whether the user has completed drawing the polygon or not. */
  private var isMarkedComplete: Boolean = false

  fun isMarkedComplete(): Boolean = isMarkedComplete

  /**
   * If the distance between the last added vertex and the given [target] is more than the
   * configured threshold, then updates the last vertex with the given [target]. Otherwise, snaps to
   * the first vertex to complete the polygon.
   */
  fun updateLastVertexAndMaybeCompletePolygon(
    target: Coordinates,
    calculateDistanceInPixels: (c1: Coordinates, c2: Coordinates) -> Double
  ) {
    check(!isMarkedComplete) { "Attempted to update last vertex after completing the drawing" }

    val firstVertex = vertices.firstOrNull()
    var updatedTarget = target
    if (firstVertex != null && vertices.size > 2) {
      val distance = calculateDistanceInPixels(firstVertex.coordinates, target)
      if (distance <= DISTANCE_THRESHOLD_DP) {
        updatedTarget = firstVertex.coordinates
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
  fun addLastVertex() {
    check(!isMarkedComplete) { "Attempted to add last vertex after completing the drawing" }
    vertices.lastOrNull()?.let { addVertex(it.coordinates, false) }
  }

  /**
   * Adds a new vertex to the polygon.
   *
   * @param vertex
   * @param shouldOverwriteLastVertex
   */
  private fun addVertex(vertex: Coordinates, shouldOverwriteLastVertex: Boolean) {
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
    check(vertices.map { it.coordinates }.isComplete()) { "Polygon is not complete" }
    check(!isMarkedComplete) { "Already marked complete" }

    isMarkedComplete = true

    refreshFeatures(vertices, true)
    setResponse(GeometryData(createGeometry(vertices, true)))
  }

  /** Returns a set of [Feature] to be drawn on map for the given [Polygon]. */
  private fun refreshFeatures(points: List<Point>, isMarkedComplete: Boolean) {
    featureFlow.value =
      if (points.isEmpty()) {
        null
      } else {
        Feature(
          id = uuidGenerator.generateUuid(),
          type = FeatureType.USER_POLYGON.ordinal,
          geometry = createGeometry(points, isMarkedComplete),
          // TODO: Set correct color.
          style = Feature.Style(Color.CYAN, Feature.VertexStyle.CIRCLE),
          clusterable = false
        )
      }
  }

  /** Returns a map geometry to be drawn based on given list of points. */
  private fun createGeometry(points: List<Point>, isMarkedComplete: Boolean): Geometry {
    val coordinates = points.map { it.coordinates }
    return if (isMarkedComplete && coordinates.isComplete()) {
      Polygon(LinearRing(coordinates))
    } else if (coordinates.isComplete()) {
      LinearRing(coordinates)
    } else {
      LineString(coordinates)
    }
  }

  companion object {
    /** Min. distance in dp between two points for them be considered as overlapping. */
    const val DISTANCE_THRESHOLD_DP = 24
  }
}
