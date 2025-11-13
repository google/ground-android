/*
 * Copyright 2023 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.point

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.getDefaultColor
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.model.submission.CaptureLocationTaskData
import org.groundplatform.android.model.submission.DropPinTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskViewModel
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.gms.getAccuracyOrNull
import org.groundplatform.android.ui.map.gms.getAltitudeOrNull
import org.groundplatform.android.ui.map.gms.toCoordinates

/** Location lock states relevant for attempting to enable it or not. */
enum class LocationLockEnabledState {
  /** The default, unknown state. */
  UNKNOWN,

  /** The location lock was already enabled, or an attempt was made. */
  ALREADY_ENABLED,

  /** The location lock was not already enabled. */
  NEEDS_ENABLE,

  /** Trigger to enable the location lock. */
  ENABLE,
}

class DropPinTaskViewModel
@Inject
constructor(
  private val uuidGenerator: OfflineUuidGenerator,
  private val localValueStore: LocalValueStore,
) : AbstractTaskViewModel() {

  private var pinColor: Int = 0
  private var lastCameraPosition: CameraPosition? = null
  private val _lastLocation = MutableStateFlow<Location?>(null)
  val features: MutableLiveData<Set<Feature>> = MutableLiveData()

  /** Whether the instructions dialog has been shown or not. */
  var instructionsDialogShown: Boolean by localValueStore::dropPinInstructionsShown

  /** Allows control for triggering the location lock programmatically. */
  private val _enableLocationLockFlow = MutableStateFlow(LocationLockEnabledState.UNKNOWN)
  val enableLocationLockFlow = _enableLocationLockFlow.asStateFlow()

  override fun initialize(job: Job, task: Task, taskData: TaskData?) {
    super.initialize(job, task, taskData)
    pinColor = job.getDefaultColor()

    // Drop a marker for current value
    when (taskData) {
      is DropPinTaskData -> dropMarker(taskData.location)
      is CaptureLocationTaskData -> dropMarker(taskData.location)
      else -> {}
    }
  }

  fun updateCameraPosition(position: CameraPosition) {
    lastCameraPosition = position
  }

  fun updateLocation(location: Location) {
    _lastLocation.update { location }
  }

  private fun updateLocationLock(newState: LocationLockEnabledState) =
    _enableLocationLockFlow.update { newState }

  fun enableLocationLock() {
    if (_enableLocationLockFlow.value == LocationLockEnabledState.NEEDS_ENABLE) {
      updateLocationLock(LocationLockEnabledState.ENABLE)
    }
  }

  suspend fun initLocationUpdates(mapViewModel: BaseMapViewModel) {
    val locationLockEnabledState =
      if (mapViewModel.hasLocationPermission()) {
        // User has permission to enable location updates, enable it now.
        mapViewModel.enableLocationLockAndGetUpdates()
        LocationLockEnabledState.ALREADY_ENABLED
      } else {
        // Otherwise, wait to enable location lock until later.
        LocationLockEnabledState.NEEDS_ENABLE
      }
    updateLocationLock(locationLockEnabledState)
    _enableLocationLockFlow.collect {
      if (it == LocationLockEnabledState.ENABLE) {
        // No-op if permission is already granted and location updates are enabled.
        mapViewModel.enableLocationLockAndGetUpdates()
        updateLocationLock(LocationLockEnabledState.ALREADY_ENABLED)
      }
    }
  }

  override fun clearResponse() {
    super.clearResponse()
    features.postValue(setOf())
  }

  private fun updateResponseWithCurrentLocation() {
    val location = _lastLocation.value
    if (location == null) {
      updateLocationLock(LocationLockEnabledState.ENABLE)
    } else {
      setValue(
        CaptureLocationTaskData(
          location = Point(location.toCoordinates()),
          altitude = location.getAltitudeOrNull(),
          accuracy = location.getAccuracyOrNull(),
        )
      )
      dropMarker(Point(location.toCoordinates()))
    }
  }

  private fun updateResponseWithCameraPosition(point: Point) {
    setValue(DropPinTaskData(point))
    dropMarker(point)
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

  fun dropPin() {
    lastCameraPosition?.let { updateResponseWithCameraPosition(Point(it.coordinates)) }
  }

  fun captureLocation() {
    updateResponseWithCurrentLocation()
  }
}
