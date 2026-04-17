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

import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.ui.common.SharedViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.AbstractMapTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.util.LocaleAwareMeasureFormatter
import org.groundplatform.android.ui.util.VibrationHelper
import org.groundplatform.android.ui.util.getDefaultColor
import org.groundplatform.android.ui.util.getFormattedArea
import org.groundplatform.android.util.distanceTo
import org.groundplatform.android.util.penult
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.LineString
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.Polygon
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.settings.MeasurementUnits
import org.groundplatform.domain.model.submission.DrawAreaTaskData
import org.groundplatform.domain.model.submission.DrawAreaTaskIncompleteData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.submission.isNotNullOrEmpty
import org.groundplatform.domain.model.task.Task
import org.groundplatform.domain.usecases.user.GetUserSettingsUseCase
import org.groundplatform.domain.util.calculateShoelacePolygonArea
import org.groundplatform.domain.util.isSelfIntersecting
import org.jetbrains.annotations.VisibleForTesting
import timber.log.Timber

/** Min. distance between the last two vertices required for distance tooltip to be shown shown. */
const val TOOLTIP_MIN_DISTANCE_METERS = 0.1

data class DrawAreaUiState(
  val tooClose: Boolean = false,
  val selfIntersection: Boolean = false,
  val showSelfIntersectionDialog: Boolean = false,
  val isMarkedComplete: Boolean = false,
  val polygonArea: String? = null,
)

@SharedViewModel
class DrawAreaTaskViewModel
@Inject
internal constructor(
  private val localValueStore: LocalValueStore,
  private val uuidGenerator: OfflineUuidGenerator,
  private val vibrationHelper: VibrationHelper,
  private val localeAwareMeasureFormatter: LocaleAwareMeasureFormatter,
  private val getUserSettingsUseCase: GetUserSettingsUseCase,
) : AbstractMapTaskViewModel() {

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

  /** Channel for one-shot camera movement events. Fragment collects to move map. */
  private val _cameraMoveEvents = Channel<Coordinates>(Channel.CONFLATED)
  val cameraMoveEvents = _cameraMoveEvents.receiveAsFlow()

  /** Whether the instructions dialog has been shown or not. */
  var instructionsDialogShown: Boolean by localValueStore::drawAreaInstructionsShown

  private val _uiState = MutableStateFlow(DrawAreaUiState())
  val uiState: StateFlow<DrawAreaUiState> = _uiState.asStateFlow()

  private var currentCameraTarget: Coordinates? = null

  /**
   * User-specified vertices of the area being drawn. If [isMarkedComplete] is false, then the last
   * vertex represents the map center and the second last vertex is the last added vertex.
   */
  private var vertices: List<Coordinates> = listOf()

  /** Stack of vertices that have been removed. */
  private val _redoVertexStack = mutableListOf<Coordinates>()
  val redoVertexStack: List<Coordinates>
    get() = _redoVertexStack

  private lateinit var featureStyle: Feature.Style
  lateinit var measurementUnits: MeasurementUnits

  override val taskActionButtonStates: StateFlow<List<ButtonActionState>> by lazy {
    combine(taskTaskData, merge(draftArea, draftUpdates), uiState) { taskData, currentFeature, ui ->
        val isClosed = (currentFeature?.geometry as? LineString)?.isClosed() ?: false
        listOfNotNull(
          getPreviousButton(),
          getSkipButton(taskData),
          getUndoButton(taskData, true),
          getRedoButton(taskData),
          getAddPointButton(isClosed, ui.tooClose),
          getCompleteButton(isClosed, ui.isMarkedComplete, ui.selfIntersection),
          getNextButton(taskData).takeIf { ui.isMarkedComplete },
        )
      }
      .distinctUntilChanged()
      .stateIn(viewModelScope, WhileSubscribed(5_000), emptyList())
  }

  override fun initialize(
    job: Job,
    task: Task,
    taskData: TaskData?,
    taskPositionInterface: TaskPositionInterface,
    surveyId: String,
  ) {
    super.initialize(job, task, taskData, taskPositionInterface, surveyId)
    viewModelScope.launch { measurementUnits = getUserSettingsUseCase.invoke().measurementUnits }
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

  fun isMarkedComplete(): Boolean = uiState.value.isMarkedComplete

  @VisibleForTesting fun getLastVertex() = vertices.lastOrNull()

  private fun onSelfIntersectionDetected() {
    _uiState.update { it.copy(showSelfIntersectionDialog = true) }
  }

  fun dismissSelfIntersectionDialog() {
    _uiState.update { it.copy(showSelfIntersectionDialog = false) }
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
    check(!uiState.value.isMarkedComplete) {
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
    val isTooClose =
      vertices.size > 1 &&
        prev?.let { calculateDistanceInPixels(it, target) <= DISTANCE_THRESHOLD_DP } == true
    _uiState.update { it.copy(tooClose = isTooClose) }

    addVertex(updatedTarget, true)
  }

  /** Attempts to remove the last vertex of drawn polygon, if any. */
  @VisibleForTesting
  fun removeLastVertex() {
    // Do nothing if there are no vertices to remove.
    if (vertices.isEmpty()) return

    // Reset complete status
    _uiState.update { it.copy(isMarkedComplete = false) }

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

    _uiState.update { it.copy(isMarkedComplete = false) }

    val redoVertex = _redoVertexStack.removeAt(_redoVertexStack.lastIndex)

    val mutableVertices = vertices.toMutableList()
    mutableVertices.add(redoVertex)
    val updatedVertices = mutableVertices.toImmutableList()

    updateVertices(updatedVertices)
    setValue(DrawAreaTaskIncompleteData(LineString(updatedVertices)))
  }

  fun onCameraMoved(newTarget: Coordinates) {
    currentCameraTarget = newTarget
  }

  /** Adds the last vertex to the polygon. */
  @VisibleForTesting
  fun addLastVertex() {
    check(!uiState.value.isMarkedComplete) {
      "Attempted to add last vertex after completing the drawing"
    }
    _redoVertexStack.clear()
    val vertex = vertices.lastOrNull() ?: currentCameraTarget
    vertex?.let {
      _uiState.update { it.copy(tooClose = vertices.size > 1) }
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

  private fun checkVertexIntersection(): Boolean {
    val intersected = isSelfIntersecting(vertices)
    _uiState.update { it.copy(selfIntersection = intersected) }
    if (intersected) {
      vertices = vertices.dropLast(1)
      onSelfIntersectionDetected()
    }
    return intersected
  }

  private fun validatePolygonCompletion(): Boolean {
    if (vertices.size < 3) {
      return false
    }

    val ring =
      if (vertices.first() != vertices.last()) {
        vertices + vertices.first()
      } else {
        vertices
      }

    val intersected = isSelfIntersecting(ring)
    _uiState.update { it.copy(selfIntersection = intersected) }
    if (intersected) {
      onSelfIntersectionDetected()
      return false
    }
    return true
  }

  private fun updateVertices(newVertices: List<Coordinates>) {
    this.vertices = newVertices
    refreshMap()
  }

  @VisibleForTesting
  fun completePolygon() {
    check(LineString(vertices).isClosed()) { "Polygon is not complete" }
    check(!uiState.value.isMarkedComplete) { "Already marked complete" }

    _uiState.update { it.copy(isMarkedComplete = true) }

    refreshMap()
    setValue(DrawAreaTaskData(Polygon(LinearRing(vertices))))
    val areaInSquareMeters = calculateShoelacePolygonArea(vertices)
    _uiState.update {
      it.copy(polygonArea = getFormattedArea(areaInSquareMeters, measurementUnits))
    }
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
  private fun refreshMap() = viewModelScope.launch {
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
    if (uiState.value.isMarkedComplete || vertices.size <= 1) return null
    val distance = vertices.penult().distanceTo(vertices.last())
    if (distance < TOOLTIP_MIN_DISTANCE_METERS) return null
    return localeAwareMeasureFormatter.formatDistance(distance, measurementUnits)
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

  private fun getRedoButton(taskData: TaskData?): ButtonActionState =
    ButtonActionState(
      action = ButtonAction.REDO,
      isEnabled = redoVertexStack.isNotEmpty() && taskData.isNotNullOrEmpty(),
      isVisible = true,
    )

  private fun getAddPointButton(isPolygonClosed: Boolean, isTooClose: Boolean): ButtonActionState =
    ButtonActionState(
      action = ButtonAction.ADD_POINT,
      isEnabled = !isPolygonClosed && !isTooClose,
      isVisible = !isPolygonClosed,
    )

  private fun getCompleteButton(
    isClosed: Boolean,
    isMarkedComplete: Boolean,
    hasSelfIntersection: Boolean,
  ): ButtonActionState =
    ButtonActionState(
      action = ButtonAction.COMPLETE,
      isEnabled = isClosed && !isMarkedComplete && !hasSelfIntersection,
      isVisible = isClosed && !isMarkedComplete,
    )

  override fun onButtonClick(action: ButtonAction) {
    when (action) {
      ButtonAction.UNDO -> {
        removeLastVertex()
        getLastVertex()?.let { _cameraMoveEvents.trySend(it) }
      }
      ButtonAction.REDO -> {
        redoLastVertex()
        getLastVertex()?.let { _cameraMoveEvents.trySend(it) }
      }
      ButtonAction.ADD_POINT -> {
        addLastVertex()
        val intersected = checkVertexIntersection()
        if (!intersected) triggerVibration()
      }
      ButtonAction.COMPLETE -> {
        if (validatePolygonCompletion()) {
          completePolygon()
        }
      }
      else -> {
        super.onButtonClick(action)
      }
    }
  }

  companion object {
    /** Min. distance in dp between two points for them be considered as overlapping. */
    const val DISTANCE_THRESHOLD_DP = 24
  }
}
