/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.geometry

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.groundplatform.android.common.Constants.ACCURACY_THRESHOLD_IN_M
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.getDefaultColor
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.submission.CaptureLocationTaskData
import org.groundplatform.android.model.submission.DrawAreaTaskData
import org.groundplatform.android.model.submission.DrawAreaTaskIncompleteData
import org.groundplatform.android.model.submission.DrawGeometryTaskData
import org.groundplatform.android.model.submission.DropPinTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.datacollection.tasks.AbstractMapTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.LocationLockEnabledState
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.gms.getAccuracyOrNull
import org.groundplatform.android.ui.map.gms.getAltitudeOrNull
import org.groundplatform.android.ui.map.gms.toCoordinates
import org.groundplatform.android.ui.util.LocaleAwareMeasureFormatter
import org.groundplatform.android.ui.util.VibrationHelper
import org.groundplatform.android.ui.util.calculateShoelacePolygonArea
import org.groundplatform.android.ui.util.getFormattedArea
import org.groundplatform.android.ui.util.isSelfIntersecting
import org.groundplatform.android.usecases.user.GetUserSettingsUseCase
import org.groundplatform.android.util.distanceTo
import org.groundplatform.android.util.penult
import timber.log.Timber

class DrawGeometryTaskViewModel
@Inject
constructor(
  private val uuidGenerator: OfflineUuidGenerator,
  private val localValueStore: LocalValueStore,
  private val vibrationHelper: VibrationHelper,
  private val localeAwareMeasureFormatter: LocaleAwareMeasureFormatter,
  private val getUserSettingsUseCase: GetUserSettingsUseCase,
) : AbstractMapTaskViewModel() {

  private val _lastLocation = MutableStateFlow<Location?>(null)
  val lastLocation = _lastLocation.asStateFlow()
  private var pinColor: Int = 0
  val features: MutableLiveData<Set<Feature>> = MutableLiveData()
  /** Whether the instructions dialog has been shown or not. */
  var instructionsDialogShown: Boolean
    get() =
      if (isDrawAreaMode()) {
        localValueStore.drawAreaInstructionsShown
      } else {
        localValueStore.dropPinInstructionsShown
      }
    set(value) {
      if (isDrawAreaMode()) {
        localValueStore.drawAreaInstructionsShown = value
      } else {
        localValueStore.dropPinInstructionsShown = value
      }
    }

  /** Polygon [Feature] being drawn by the user. */
  private val _draftArea: MutableStateFlow<Feature?> = MutableStateFlow(null)
  val draftArea: StateFlow<Feature?> = _draftArea.asStateFlow()

  /** Unique identifier for the currently active draft polygon or line being drawn. */
  private var draftTag: Feature.Tag? = null

  private val _draftUpdates = MutableSharedFlow<Feature>(extraBufferCapacity = 1)
  val draftUpdates = _draftUpdates.asSharedFlow()

  private val _polygonArea = MutableLiveData<String>()
  val polygonArea: LiveData<String> = _polygonArea

  private var currentCameraTarget: Coordinates? = null
  private var vertices: List<Coordinates> = listOf()
  private val _redoVertexStack = mutableListOf<Coordinates>()
  val redoVertexStack: List<Coordinates>
    get() = _redoVertexStack

  private val _isMarkedComplete = MutableStateFlow(false)
  val isMarkedComplete: StateFlow<Boolean> = _isMarkedComplete.asStateFlow()

  private val _isTooClose = MutableStateFlow(false)
  val isTooClose: StateFlow<Boolean> = _isTooClose.asStateFlow()

  private val _showSelfIntersectionDialog = MutableSharedFlow<Unit>()
  var hasSelfIntersection: Boolean = false
    private set

  private lateinit var featureStyle: Feature.Style
  private lateinit var measurementUnits: MeasurementUnits

  val isCaptureEnabled: Flow<Boolean> =
    _lastLocation.map { location ->
      val accuracy: Float = location?.getAccuracyOrNull()?.toFloat() ?: Float.MAX_VALUE
      location != null && accuracy <= getAccuracyThreshold()
    }

  override fun initialize(job: Job, task: Task, taskData: TaskData?) {
    super.initialize(job, task, taskData)
    viewModelScope.launch { measurementUnits = getUserSettingsUseCase.invoke().measurementUnits }
    pinColor = job.getDefaultColor()
    featureStyle = Feature.Style(job.getDefaultColor(), Feature.VertexStyle.CIRCLE)

    if (isDrawAreaMode()) {
      initializeDrawArea(taskData)
    } else {
      initializeDropPin(taskData)
    }
  }

  private fun initializeDrawArea(taskData: TaskData?) {
    if (taskData == null) return
    when (taskData) {
      is DrawAreaTaskIncompleteData -> {
        updateVertices(taskData.lineString.coordinates)
      }
      is DrawAreaTaskData -> {
        updateVertices(taskData.area.getShellCoordinates())
        try {
          completePolygon()
        } catch (e: IllegalStateException) {
          Timber.e(e, "Error when loading draw area from saved state")
          updateVertices(listOf())
        }
      }
      is DrawGeometryTaskData -> {
        if (taskData.geometry is Polygon) {
          updateVertices(taskData.geometry.getShellCoordinates())
          try {
            completePolygon()
          } catch (e: IllegalStateException) {
            Timber.e(e, "Error when loading draw area from saved state")
            updateVertices(listOf())
          }
        } else if (taskData.geometry is LineString) {
          updateVertices(taskData.geometry.coordinates)
        }
      }
    }
  }

  private fun initializeDropPin(taskData: TaskData?) {
    val geometry =
      (taskData as? DropPinTaskData)?.location
        ?: (taskData as? CaptureLocationTaskData)?.location
        ?: (taskData as? DrawGeometryTaskData)?.geometry as? Point

    if (geometry != null) {
      dropMarker(geometry)
    } else if (isLocationLockRequired()) {
      updateLocationLock(LocationLockEnabledState.ENABLE)
    }
  }

  fun isDrawAreaMode(): Boolean = task.drawGeometry?.allowedMethods?.contains("DRAW_AREA") == true

  fun isLocationLockRequired(): Boolean = task.drawGeometry?.isLocationLockRequired ?: false

  private fun getAccuracyThreshold(): Float =
    task.drawGeometry?.minAccuracyMeters ?: ACCURACY_THRESHOLD_IN_M

  fun updateLocation(location: Location) {
    _lastLocation.update { location }
  }

  fun onCaptureLocation() {
    val location = _lastLocation.value
    if (location == null) {
      updateLocationLock(LocationLockEnabledState.ENABLE)
    } else {
      val accuracy = location.getAccuracyOrNull()
      val threshold = getAccuracyThreshold()
      if (accuracy != null && accuracy > threshold) {
        error("Location accuracy $accuracy exceeds threshold $threshold")
      }

      val point = Point(location.toCoordinates())
      setValue(
        CaptureLocationTaskData(
          location = point,
          altitude = location.getAltitudeOrNull(),
          accuracy = accuracy,
        )
      )
      dropMarker(point)
    }
  }

  fun onDropPin() {
    getLastCameraPosition()?.let {
      val point = Point(it.coordinates)
      setValue(DrawGeometryTaskData(point))
      dropMarker(point)
    }
  }

  // Draw Area Methods
  fun isMarkedComplete(): Boolean = isMarkedComplete.value

  private fun onSelfIntersectionDetected() {
    viewModelScope.launch { _showSelfIntersectionDialog.emit(Unit) }
  }

  fun getLastVertex() = vertices.lastOrNull()

  fun removeLastVertex() {
    if (vertices.isEmpty()) return
    _isMarkedComplete.value = false
    _redoVertexStack.add(vertices.last())
    val updatedVertices = vertices.toMutableList().apply { removeAt(lastIndex) }.toImmutableList()
    updateVertices(updatedVertices)
    if (updatedVertices.isEmpty()) {
      setValue(null)
      _redoVertexStack.clear()
    } else {
      setValue(DrawGeometryTaskData(LineString(updatedVertices)))
    }
  }

  fun redoLastVertex() {
    if (redoVertexStack.isEmpty()) {
      Timber.e("redoVertexStack is already empty")
      return
    }
    _isMarkedComplete.value = false
    val redoVertex = _redoVertexStack.removeAt(_redoVertexStack.lastIndex)
    val updatedVertices = vertices.toMutableList().apply { add(redoVertex) }.toImmutableList()
    updateVertices(updatedVertices)
    setValue(DrawGeometryTaskData(LineString(updatedVertices)))
  }

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
      vertices.size > 1 &&
        prev?.let { calculateDistanceInPixels(it, target) <= DISTANCE_THRESHOLD_DP } == true

    addVertex(updatedTarget, true)
  }

  fun onCameraMoved(newTarget: Coordinates) {
    currentCameraTarget = newTarget
  }

  fun addLastVertex() {
    check(!isMarkedComplete.value) { "Attempted to add last vertex after completing the drawing" }
    _redoVertexStack.clear()
    val vertex = vertices.lastOrNull() ?: currentCameraTarget
    vertex?.let {
      _isTooClose.value = vertices.size > 1
      addVertex(it, false)
    }
  }

  private fun addVertex(vertex: Coordinates, shouldOverwriteLastVertex: Boolean) {
    val updatedVertices = vertices.toMutableList()
    if (shouldOverwriteLastVertex && updatedVertices.isNotEmpty()) {
      updatedVertices.removeAt(updatedVertices.lastIndex)
    }
    updatedVertices.add(vertex)
    updateVertices(updatedVertices.toImmutableList())

    if (!shouldOverwriteLastVertex) {
      setValue(DrawGeometryTaskData(LineString(updatedVertices.toImmutableList())))
    }
  }

  fun validatePolygonCompletion(): Boolean {
    if (vertices.size < 3) return false
    val ring = if (vertices.first() != vertices.last()) vertices + vertices.first() else vertices
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
    setValue(DrawGeometryTaskData(Polygon(LinearRing(vertices))))

    val areaInSquareMeters = calculateShoelacePolygonArea(vertices)
    _polygonArea.value = getFormattedArea(areaInSquareMeters, measurementUnits)
  }

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

  private fun getDistanceTooltipText(): String? {
    if (isMarkedComplete.value || vertices.size <= 1) return null
    val distance = vertices.penult().distanceTo(vertices.last())
    if (distance < TOOLTIP_MIN_DISTANCE_METERS) return null
    return localeAwareMeasureFormatter.formatDistance(distance, measurementUnits)
  }

  override fun clearResponse() {
    super.clearResponse()
    features.postValue(setOf())
  }

  private fun dropMarker(point: Point) =
    viewModelScope.launch {
      val feature = createFeature(point)
      features.postValue(setOf(feature))
    }

  /** Creates a new map [Feature] representing the point placed by the user. */
  private suspend fun createFeature(point: Point): Feature =
    Feature(
      id = uuidGenerator.generateUuid(),
      type = Feature.Type.USER_POINT,
      geometry = point,
      style = Feature.Style(pinColor),
      clusterable = false,
      selected = true,
    )

  fun checkVertexIntersection(): Boolean {
    hasSelfIntersection = isSelfIntersecting(vertices)
    if (hasSelfIntersection) {
      val updatedVertices = vertices.dropLast(1)
      updateVertices(updatedVertices)
      onSelfIntersectionDetected()
    }
    return hasSelfIntersection
  }

  fun triggerVibration() {
    vibrationHelper.vibrate()
  }

  fun shouldShowInstructionsDialog() = !instructionsDialogShown && !isLocationLockRequired()

  companion object {
    const val DISTANCE_THRESHOLD_DP = 24
    const val TOOLTIP_MIN_DISTANCE_METERS = 0.1
  }
}
