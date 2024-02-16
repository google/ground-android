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
import com.google.android.ground.R
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.Value
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.FeatureType
import com.google.android.ground.ui.util.ColorUtil
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@SharedViewModel
class DrawAreaTaskViewModel
@Inject
internal constructor(
  private val uuidGenerator: OfflineUuidGenerator,
  private val resources: Resources,
  private val colorUtil: ColorUtil
) : AbstractTaskViewModel(resources) {

  /** Polygon [Feature] being drawn by the user. */
  private val _draftArea: MutableStateFlow<Feature?> = MutableStateFlow(null)
  val draftArea: StateFlow<Feature?> =
    _draftArea.stateIn(viewModelScope, started = SharingStarted.Lazily, null)

  /**
   * User-specified vertices of the area being drawn. If [isMarkedComplete] is false, then the last
   * vertex represents the map center and the second last vertex is the last added vertex.
   */
  private var vertices: List<Coordinates> = listOf()

  /** Represents whether the user has completed drawing the polygon or not. */
  private var isMarkedComplete: Boolean = false

  private var strokeColor: Int = 0

  override fun initialize(job: Job, task: Task, value: Value?) {
    super.initialize(job, task, value)
    strokeColor = colorUtil.getColor(R.color.drawAreaLineStringStrokeColor)
  }

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
      val distance = calculateDistanceInPixels(firstVertex, target)
      if (distance <= DISTANCE_THRESHOLD_DP) {
        updatedTarget = firstVertex
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
    val updatedVertices = vertices.toMutableList().apply { removeLast() }.toImmutableList()

    // Render changes to UI
    updateVertices(updatedVertices)

    // Update saved response.
    if (updatedVertices.isEmpty()) {
      setValue(null)
    } else {
      setValue(DrawAreaTaskIncompleteResult(LineString(updatedVertices)))
    }
  }

  /** Adds the last vertex to the polygon. */
  fun addLastVertex() {
    check(!isMarkedComplete) { "Attempted to add last vertex after completing the drawing" }
    vertices.lastOrNull()?.let { addVertex(it, false) }
  }

  /** Adds a new vertex to the polygon. */
  private fun addVertex(vertex: Coordinates, shouldOverwriteLastVertex: Boolean) {
    val updatedVertices = vertices.toMutableList()

    // Maybe remove the last vertex before adding the new vertex.
    if (shouldOverwriteLastVertex && updatedVertices.isNotEmpty()) {
      updatedVertices.removeLast()
    }

    // Add the new vertex
    updatedVertices.add(vertex)

    // Render changes to UI
    updateVertices(updatedVertices.toImmutableList())

    // Save response iff it is user initiated
    if (!shouldOverwriteLastVertex) {
      setValue(DrawAreaTaskIncompleteResult(LineString(updatedVertices.toImmutableList())))
    }
  }

  private fun updateVertices(newVertices: List<Coordinates>) {
    this.vertices = newVertices
    refreshMap()
  }

  fun onCompletePolygonButtonClick() {
    check(LineString(vertices).isClosed()) { "Polygon is not complete" }
    check(!isMarkedComplete) { "Already marked complete" }

    isMarkedComplete = true

    refreshMap()
    setValue(DrawAreaTaskResult(Polygon(LinearRing(vertices))))
  }

  /** Updates the [Feature] drawn on map based on the value of [vertices]. */
  private fun refreshMap() {
    _draftArea.value =
      if (vertices.isEmpty()) {
        null
      } else {
        Feature(
          id = uuidGenerator.generateUuid(),
          type = FeatureType.USER_POLYGON.ordinal,
          geometry = LineString(vertices),
          style = Feature.Style(strokeColor, Feature.VertexStyle.CIRCLE),
          clusterable = false,
          selected = true
        )
      }
  }

  override fun validate(task: Task, value: Value?): String? {
    // Invalid response for draw area task.
    if (task.type == Task.Type.DRAW_AREA && value is DrawAreaTaskIncompleteResult) {
      return resources.getString(R.string.incomplete_area)
    }
    return super.validate(task, value)
  }

  companion object {
    /** Min. distance in dp between two points for them be considered as overlapping. */
    const val DISTANCE_THRESHOLD_DP = 24
  }
}
