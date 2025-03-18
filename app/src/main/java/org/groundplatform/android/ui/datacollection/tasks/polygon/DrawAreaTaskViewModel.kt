/*
 * Copyright 2024 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.polygon

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.getDefaultColor
import org.groundplatform.android.model.submission.DrawAreaTaskData
import org.groundplatform.android.model.submission.DrawAreaTaskIncompleteData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.persistence.local.LocalValueStore
import org.groundplatform.android.persistence.uuid.OfflineUuidGenerator
import org.groundplatform.android.ui.common.SharedViewModel
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskViewModel
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.FeatureType
import org.groundplatform.android.ui.util.VibrationHelper
import org.groundplatform.android.ui.util.calculateShoelacePolygonArea
import org.groundplatform.android.ui.util.isSelfIntersecting
import timber.log.Timber

@SharedViewModel
class DrawAreaTaskViewModel
@Inject
internal constructor(
  private val localValueStore: LocalValueStore,
  private val uuidGenerator: OfflineUuidGenerator,
  private val vibrationHelper: VibrationHelper,
) : AbstractTaskViewModel() {

  /** Polygon [Feature] being drawn by the user. */
  private val _draftArea: MutableStateFlow<Feature?> = MutableStateFlow(null)
  val draftArea: StateFlow<Feature?> = _draftArea.asStateFlow()

  /** Whether the instructions dialog has been shown or not. */
  var instructionsDialogShown: Boolean by localValueStore::drawAreaInstructionsShown

  private val _polygonArea = MutableLiveData<Double>()
  val polygonArea: LiveData<Double> = _polygonArea

  /**
   * User-specified vertices of the area being drawn. If [isMarkedComplete] is false, then the last
   * vertex represents the map center and the second last vertex is the last added vertex.
   */
  private var vertices: List<Coordinates> = listOf()

  /** Represents whether the user has completed drawing the polygon or not. */
  private var isMarkedComplete: Boolean = false

  private val _showSelfIntersectionDialog = MutableSharedFlow<Unit>()
  val showSelfIntersectionDialog = _showSelfIntersectionDialog.asSharedFlow()

  private var strokeColor: Int = 0

  override fun initialize(job: Job, task: Task, taskData: TaskData?) {
    super.initialize(job, task, taskData)
    strokeColor = job.getDefaultColor()
    (taskData as? DrawAreaTaskData)?.let {
      updateVertices(it.area.getShellCoordinates())
      try {
        completePolygon()
      } catch (e: IllegalStateException) {
        // This state can theoretically happen if the coordinates form an incomplete ring, but
        // construction of a DrawAreaTaskData is impossible without a complete ring anyway so it is
        // unlikely to happen. This can also happen if `isMarkedComplete` is true at initialization
        // time, which is also unlikely.
        Timber.e(e, "Error when loading draw area from saved state")
        updateVertices(listOf())
      }
    }
  }

  fun isMarkedComplete(): Boolean = isMarkedComplete

  fun getLastVertex() = vertices.lastOrNull()

  private fun onSelfIntersectionDetected() {
    viewModelScope.launch { _showSelfIntersectionDialog.emit(Unit) }
  }

  /**
   * If the distance between the last added vertex and the given [target] is more than the
   * configured threshold, then updates the last vertex with the given [target]. Otherwise, snaps to
   * the first vertex to complete the polygon.
   */
  fun updateLastVertexAndMaybeCompletePolygon(
    target: Coordinates,
    calculateDistanceInPixels: (c1: Coordinates, c2: Coordinates) -> Double,
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
      setValue(DrawAreaTaskIncompleteData(LineString(updatedVertices)))
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

    // Save response if it is user initiated
    if (!shouldOverwriteLastVertex) {
      setValue(DrawAreaTaskIncompleteData(LineString(updatedVertices.toImmutableList())))
    }
  }

  fun checkVertexIntersection() {
    if (isSelfIntersecting(vertices)) {
      vertices = vertices.dropLast(1)
      onSelfIntersectionDetected()
    }
  }

  private fun updateVertices(newVertices: List<Coordinates>) {
    this.vertices = newVertices
    refreshMap()
  }

  fun completePolygon() {
    check(LineString(vertices).isClosed()) { "Polygon is not complete" }
    check(!isMarkedComplete) { "Already marked complete" }

    isMarkedComplete = true

    refreshMap()
    setValue(DrawAreaTaskData(Polygon(LinearRing(vertices))))
    _polygonArea.value = calculateShoelacePolygonArea(vertices)
  }

  /** Updates the [Feature] drawn on map based on the value of [vertices]. */
  private fun refreshMap() =
    viewModelScope.launch {
      _draftArea.emit(
        if (vertices.isEmpty()) {
          null
        } else {
          Feature(
            id = uuidGenerator.generateUuid(),
            type = FeatureType.USER_POLYGON.ordinal,
            geometry = LineString(vertices),
            style = Feature.Style(strokeColor, Feature.VertexStyle.CIRCLE),
            clusterable = false,
            selected = true,
          )
        }
      )
    }

  override fun validate(task: Task, taskData: TaskData?): Int? {
    // Invalid response for draw area task.
    if (task.type == Task.Type.DRAW_AREA && taskData is DrawAreaTaskIncompleteData) {
      return R.string.incomplete_area
    }
    return super.validate(task, taskData)
  }

  fun triggerVibration() {
    vibrationHelper.vibrate()
  }

  companion object {
    /** Min. distance in dp between two points for them be considered as overlapping. */
    const val DISTANCE_THRESHOLD_DP = 24
  }
}
