/*
 * Copyright 2020 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.groundplatform.android.R
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.SkippedTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.isNullOrEmpty
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.ui.common.BaseMapViewModel

/** Defines the state of an inflated [Task] and controls its UI. */
open class AbstractTaskViewModel internal constructor() : AbstractViewModel() {

  /** Current value. */
  private val _taskDataFlow: MutableStateFlow<TaskData?> = MutableStateFlow(null)
  val taskTaskData: StateFlow<TaskData?> = _taskDataFlow.asStateFlow()

  lateinit var task: Task

  /** Allows control for triggering the location lock programmatically. */
  private val _enableLocationLockFlow = MutableStateFlow(LocationLockEnabledState.UNKNOWN)
  val enableLocationLockFlow = _enableLocationLockFlow.asStateFlow()

  open fun initialize(job: Job, task: Task, taskData: TaskData?) {
    this.task = task
    setValue(taskData)
  }

  /** Checks if the current value is valid and updates error value. */
  fun validate(): Int? = validate(task, taskTaskData.value)

  /**
   * Performs input validation on the given [Task] and associated [TaskData].
   *
   * Returns an [Int] identifier for an error string if validation fails, returns null otherwise.
   * Subclasses may override this method to validate input data and display an error message to the
   * user.
   */
  open fun validate(task: Task, taskData: TaskData?): Int? {
    // Empty response for a required task.
    if (task.isRequired && (taskData == null || taskData.isEmpty())) {
      return R.string.required_task
    }
    return null
  }

  fun setSkipped() {
    setValue(SkippedTaskData())
  }

  fun setValue(taskData: TaskData?) {
    _taskDataFlow.update { taskData }
  }

  open fun clearResponse() {
    setValue(null)
  }

  fun isTaskOptional(): Boolean = !task.isRequired

  fun hasNoData(): Boolean = taskTaskData.value.isNullOrEmpty()

  fun updateLocationLock(newState: LocationLockEnabledState) =
    _enableLocationLockFlow.update { newState }

  fun enableLocationLock() {
    if (_enableLocationLockFlow.value == LocationLockEnabledState.NEEDS_ENABLE) {
      updateLocationLock(LocationLockEnabledState.ENABLE)
    }
  }

  // TODO: Investigate if this method be pulled to BasemapViewModel since location lock is available
  // Issue URL: https://github.com/google/ground-android/issues/2985
  //  for all map tasks.
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
}

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
