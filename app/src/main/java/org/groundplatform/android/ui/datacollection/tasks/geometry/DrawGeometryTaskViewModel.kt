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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.groundplatform.android.common.Constants.ACCURACY_THRESHOLD_IN_M
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.getDefaultColor
import org.groundplatform.android.model.submission.CaptureLocationTaskData
import org.groundplatform.android.model.submission.DropPinTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.datacollection.tasks.AbstractMapTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.LocationLockEnabledState
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.gms.getAccuracyOrNull
import org.groundplatform.android.ui.map.gms.getAltitudeOrNull
import org.groundplatform.android.ui.map.gms.toCoordinates

class DrawGeometryTaskViewModel
@Inject
constructor(
  private val uuidGenerator: OfflineUuidGenerator,
  private val localValueStore: LocalValueStore,
) : AbstractMapTaskViewModel() {

  private val _lastLocation = MutableStateFlow<Location?>(null)
  val lastLocation = _lastLocation.asStateFlow()
  private var pinColor: Int = 0
  val features: MutableLiveData<Set<Feature>> = MutableLiveData()
  /** Whether the instructions dialog has been shown or not. */
  var instructionsDialogShown: Boolean by localValueStore::dropPinInstructionsShown

  val isCaptureEnabled: Flow<Boolean> =
    _lastLocation.map { location ->
      val accuracy: Float = location?.getAccuracyOrNull()?.toFloat() ?: Float.MAX_VALUE
      location != null && accuracy <= getAccuracyThreshold()
    }

  override fun initialize(job: Job, task: Task, taskData: TaskData?) {
    super.initialize(job, task, taskData)
    pinColor = job.getDefaultColor()

    if (isLocationLockRequired()) {
      updateLocationLock(LocationLockEnabledState.ENABLE)
    }

    // Drop a marker for current value if Drop Pin mode (or even capture location mode if we want to
    // show it?)
    // In CaptureLocation, we don't drop a marker.
    // In DropPin, we do.
    if (!isLocationLockRequired()) {
      (taskData as? DropPinTaskData)?.let { dropMarker(it.location) }
    }
  }

  fun isLocationLockRequired(): Boolean = task.drawGeometry?.isLocationLockRequired ?: false

  private fun getAccuracyThreshold(): Float =
    task.drawGeometry?.minAccuracyMeters ?: ACCURACY_THRESHOLD_IN_M.toFloat()

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
        // Logic to handle poor accuracy?
        // CaptureLocationTaskViewModel throws error here, but UI should prevent click.
        error("Location accuracy $accuracy exceeds threshold $threshold")
      }

      // We save as CaptureLocationTaskData? Or DropPinTaskData?
      // Since it's a unified task, we might want to use a unified data type or reuse existing.
      // If we use DrawGeometryTaskData, it doesn't exist yet.
      // If we use CaptureLocationTaskData, we might break existing DropPin tasks if they convert.
      // However, DropPin tasks use DropPinTaskData.

      // For now, let's use DropPinTaskData for everything since DrawGeometry is generalized
      // DropPin.
      // Use CaptureLocationTaskData if it is strictly capture location?
      // Wait, DropPinTaskData is just a point. CaptureLocationTaskData is point + altitude +
      // accuracy.

      // If we require device location, we likely want the metadata (accuracy).
      // If we drop pin, we just want the point.

      // Let's use CaptureLocationTaskData if location lock is required?
      // But DropPin tasks (legacy) used DrawGeometry proto but mapped to DropPin task type.
      // Now they are DrawGeometry task type.

      // If I return CaptureLocationTaskData, will it be compatible?
      // The task type is DRAW_GEOMETRY.
      // Submission data must match task type?
      // existing CaptureLocation tasks use CAPTURE_LOCATION type.
      // existing DropPin tasks use DROP_PIN type.
      // NEW tasks use DRAW_GEOMETRY type.

      // If we use DRAW_GEOMETRY task type, we need a corresponding Submission Data type?
      // Or can we re-use?
      // TaskData is a sealed class.
      // I should check `TaskData` definition.

      setValue(
        CaptureLocationTaskData(
          location = Point(location.toCoordinates()),
          altitude = location.getAltitudeOrNull(),
          accuracy = accuracy,
        )
      )
    }
  }

  fun onDropPin() {
    getLastCameraPosition()?.let {
      val point = Point(it.coordinates)
      setValue(DropPinTaskData(point))
      dropMarker(point)
    }
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

  fun shouldShowInstructionsDialog() = !instructionsDialogShown && !isLocationLockRequired()
}
