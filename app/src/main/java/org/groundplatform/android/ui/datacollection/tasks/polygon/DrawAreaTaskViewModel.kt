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

import android.icu.util.MeasureUnit
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
import org.groundplatform.android.common.Constants
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
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
import org.groundplatform.android.ui.common.SharedViewModel
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskViewModel
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.util.LocaleAwareMeasureFormatter
import org.groundplatform.android.ui.util.VibrationHelper
import org.groundplatform.android.ui.util.calculateShoelacePolygonArea
import org.groundplatform.android.ui.util.isSelfIntersecting
import org.groundplatform.android.util.distanceTo
import org.groundplatform.android.util.penult
import timber.log.Timber

/** Min. distance between the last two vertices required for distance tooltip to be shown shown. */
const val TOOLTIP_MIN_DISTANCE_METERS = 0.1

@SharedViewModel
class DrawAreaTaskViewModel
@Inject
internal constructor(
  private val localValueStore: LocalValueStore,
  private val uuidGenerator: OfflineUuidGenerator,
  private val vibrationHelper: VibrationHelper,
  private val localeAwareMeasureFormatter: LocaleAwareMeasureFormatter,
) : AbstractTaskViewModel() {

  /** Polygon [Feature] being drawn by the user. */
  private val _draftArea: MutableStateFlow<Feature?> = MutableStateFlow(null)
  val draftArea: StateFlow<Feature?> = _draftArea.asStateFlow()

  /**
   * Unique identifier for the currently active draft polygon or line being drawn.
   *
   * This tag helps the ViewModel distinguish between multiple user-created features and ensures
   * that updates are applied to the correct draft feature until completion.
   */
  private var draftTag: Feature.Tag? = null

  /**
   * Emits incremental updates to the currently drawn draft feature (e.g., polygon or line string).
   *
   * The flow sends partial geometry updates—such as when a new vertex is added or moved— allowing
   * the map UI to update the in-progress shape in real-time.
   *
   * Uses [MutableSharedFlow] with a small buffer to avoid missing updates during rapid emissions.
   */
  private val _draftUpdates = MutableSharedFlow<Feature>(extraBufferCapacity = 1)

  /**
   * Public read-only access to the stream of draft feature updates.
   *
   * UI components (e.g., map fragments) collect from this flow to render live geometry updates as
   * the user draws or modifies a shape.
   */
  val draftUpdates = _draftUpdates.asSharedFlow()

  /** Whether the instructions dialog has been shown or not. */
  var instructionsDialogShown: Boolean by localValueStore::drawAreaInstructionsShown

  private val _polygonArea = MutableLiveData<Double>()
  val polygonArea: LiveData<Double> = _polygonArea

  /**
   * User-specified vertices of the area being drawn. If [isMarkedComplete] is false, then the last
   * vertex represents the map center and the second last vertex is the last added vertex.
   */
  private var vertices: List<Coordinates> = listOf()

  /** Stack of vertices that have been removed. */
  private val _redoVertexStack = mutableListOf<Coordinates>()
  val redoVertexStack: List<Coordinates>
    get() = _redoVertexStack

  /** Represents whether the user has completed drawing the polygon or not. */
  private val _isMarkedComplete = MutableStateFlow(false)
  val isMarkedComplete: StateFlow<Boolean> = _isMarkedComplete.asStateFlow()

  private val _isTooClose = MutableStateFlow(false)
  val isTooClose: StateFlow<Boolean> = _isTooClose.asStateFlow()

  private val _showSelfIntersectionDialog = MutableSharedFlow<Unit>()
  val showSelfIntersectionDialog = _showSelfIntersectionDialog.asSharedFlow()

  var hasSelfIntersection: Boolean = false
    private set

  private lateinit var featureStyle: Feature.Style

  override fun initialize(job: Job, task: Task, taskData: TaskData?) {
    super.initialize(job, task, taskData)
    featureStyle = Feature.Style(job.getDefaultColor(), Feature.VertexStyle.CIRCLE)

    // Apply saved state if it exists.
    when (taskData) {
      is DrawAreaTaskIncompleteData -> {
        updateVertices(taskData.lineString.coordinates)
      }

      is DrawAreaTaskData -> {
        updateVertices(taskData.area.getShellCoordinates())
        try {
          completePolygon()
        } catch (e: IllegalStateException) {
          // This state can theoretically happen if the coordinates form an incomplete ring, but
          // construction of a DrawAreaTaskData is impossible without a complete ring anyway so it
          // is
          // unlikely to happen. This can also happen if `isMarkedComplete` is true at
          // initialization
          // time, which is also unlikely.
          Timber.e(e, "Error when loading draw area from saved state")
          updateVertices(listOf())
        }
      }
    }
  }

  fun isMarkedComplete(): Boolean = isMarkedComplete.value

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
    check(!isMarkedComplete.value) {
      "Attempted to update last vertex after completing the drawing"
    }

    val firstVertex = vertices.firstOrNull()
    var updatedTarget = target
    if (firstVertex != null && vertices.size > 2) {
      val distance = calculateDistanceInPixels(firstVertex, target)

      if (distance <= DISTANCE_THRESHOLD_DP) {
        updatedTarget = firstVertex
      }
    }

    val prev = vertices.dropLast(1).lastOrNull()
    _isTooClose.value =
      prev?.let { calculateDistanceInPixels(it, target) <= DISTANCE_THRESHOLD_DP } == true

    addVertex(updatedTarget, true)
  }

  /** Attempts to remove the last vertex of drawn polygon, if any. */
  fun removeLastVertex() {
    // Do nothing if there are no vertices to remove.
    if (vertices.isEmpty()) return

    // Reset complete status
    _isMarkedComplete.value = false

    _redoVertexStack.add(vertices.last())

    // Remove last vertex and update polygon
    val updatedVertices = vertices.toMutableList().apply { removeAt(lastIndex) }.toImmutableList()

    // Render changes to UI
    updateVertices(updatedVertices)

    // Update saved response.
    if (updatedVertices.isEmpty()) {
      setValue(null)
      _redoVertexStack.clear()
    } else {
      setValue(DrawAreaTaskIncompleteData(LineString(updatedVertices)))
    }
  }

  fun redoLastVertex() {
    if (redoVertexStack.isEmpty()) {
      Timber.e("redoVertexStack is already empty")
      return
    }

    _isMarkedComplete.value = false

    val redoVertex = _redoVertexStack.removeAt(_redoVertexStack.lastIndex)

    val mutableVertices = vertices.toMutableList()
    mutableVertices.add(redoVertex)
    val updatedVertices = mutableVertices.toImmutableList()

    updateVertices(updatedVertices)
    setValue(DrawAreaTaskIncompleteData(LineString(updatedVertices)))
  }

  /** Adds the last vertex to the polygon. */
  fun addLastVertex() {
    check(!isMarkedComplete.value) { "Attempted to add last vertex after completing the drawing" }
    _redoVertexStack.clear()
    vertices.lastOrNull()?.let {
      _isTooClose.value = true
      addVertex(it, false)
    }
  }

  /** Adds a new vertex to the polygon. */
  private fun addVertex(vertex: Coordinates, shouldOverwriteLastVertex: Boolean) {
    val updatedVertices = vertices.toMutableList()

    // Maybe remove the last vertex before adding the new vertex.
    if (shouldOverwriteLastVertex && updatedVertices.isNotEmpty()) {
      updatedVertices.removeAt(updatedVertices.lastIndex)
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

  fun checkVertexIntersection(): Boolean {
    hasSelfIntersection = isSelfIntersecting(vertices)
    if (hasSelfIntersection) {
      vertices = vertices.dropLast(1)
      onSelfIntersectionDetected()
    }
    return hasSelfIntersection
  }

  fun validatePolygonCompletion(): Boolean {
    if (vertices.size < 3) {
      return false
    }

    val ring =
      if (vertices.first() != vertices.last()) {
        vertices + vertices.first()
      } else {
        vertices
      }

    hasSelfIntersection = isSelfIntersecting(ring)
    if (hasSelfIntersection) {
      onSelfIntersectionDetected()
      return false
    }
    return true
  }

  private fun updateVertices(newVertices: List<Coordinates>) {
    this.vertices = newVertices
    refreshMap()
  }

  fun completePolygon() {
    check(LineString(vertices).isClosed()) { "Polygon is not complete" }
    check(!isMarkedComplete.value) { "Already marked complete" }

    _isMarkedComplete.value = true

    refreshMap()
    setValue(DrawAreaTaskData(Polygon(LinearRing(vertices))))
    _polygonArea.value = calculateShoelacePolygonArea(vertices)
  }

  /**
   * Emits the current draft polygon or line feature state to the map.
   *
   * This function is responsible for keeping the map view in sync with the user's drawing
   * interactions:
   * - When vertices are empty → clears the current draft from the map.
   * - On the first vertex → creates a new [Feature] and emits it to [_draftArea].
   * - On subsequent updates → reuses the same [Feature.Tag] and emits updated geometry through
   *   [_draftUpdates] for in-place map updates.
   *
   * The goal is to ensure smooth, flicker-free rendering by avoiding unnecessary feature
   * re-creation. Only the geometry and style of the active draft are updated in place on the map.
   *
   * This coroutine runs on [viewModelScope] to ensure lifecycle safety.
   */
  private fun refreshMap() =
    viewModelScope.launch {
      if (vertices.isEmpty()) {
        _draftArea.emit(null)
        draftTag = null
      } else {
        if (draftTag == null) {
          val feature = buildPolygonFeature()
          draftTag = feature.tag
          _draftArea.emit(feature)
        } else {
          val feature = buildPolygonFeature(id = draftTag!!.id)
          _draftUpdates.tryEmit(feature)
        }
      }
    }

  private suspend fun buildPolygonFeature(id: String? = null) =
    Feature(
      id = id ?: uuidGenerator.generateUuid(),
      type = Feature.Type.USER_POLYGON,
      geometry = LineString(vertices),
      style = featureStyle,
      clusterable = false,
      selected = true,
      tooltipText = getDistanceTooltipText(),
    )

  /** Returns the distance in meters between the last two vertices for displaying in the tooltip. */
  private fun getDistanceTooltipText(): String? {
    if (isMarkedComplete.value || vertices.size <= 1) return null
    val distance = vertices.penult().distanceTo(vertices.last())
    if (distance < TOOLTIP_MIN_DISTANCE_METERS) return null
    return localeAwareMeasureFormatter.formatDistance(distance, getLengthUnit())
  }

  private fun getLengthUnit(): MeasureUnit {
    val unit = localValueStore.selectedLengthUnit
    return when (unit) {
      Constants.LENGTH_UNIT_METER -> MeasureUnit.METER
      Constants.LENGTH_UNIT_FEET -> MeasureUnit.FOOT
      else -> error("Unknown distance unit: $unit")
    }
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
