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
package org.groundplatform.android.ui.datacollection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.model.task.TaskSelections

/**
 * Manages the state of [TaskData] associated with [Task] instances.
 *
 * This class provides methods to set, retrieve, and observe the data associated with tasks.
 */
class TaskDataHandler {

  private val _dataState = MutableStateFlow(LinkedHashMap<Task, TaskData?>())

  val dataState: StateFlow<Map<Task, TaskData?>>
    get() = _dataState.asStateFlow()

  /**
   * Sets the [TaskData] for a given [Task].
   *
   * If the new value is the same as the current value, no update is performed.
   *
   * @param task The [Task] for which to set the data.
   * @param newValue The new [TaskData] value for the task.
   */
  fun setData(task: Task, newValue: TaskData?) {
    if (getData(task) == newValue) return
    // Ensure that the map is recreated to ensure that the state flow is emitted.
    _dataState.value = LinkedHashMap(_dataState.value).apply { this[task] = newValue }
  }

  /**
   * Retrieves the [TaskData] associated with a given [Task].
   *
   * @param task The [Task] to retrieve data for.
   * @return The [TaskData] associated with the task, or `null` if no data is found.
   */
  fun getData(task: Task): TaskData? = _dataState.value[task]

  /**
   * Returns a [TaskSelections] map representing the current state of task data.
   *
   * This method allows for an optional override of a specific task's value.
   *
   * @param taskValueOverride An optional pair of task ID and [TaskData] to override.
   * @return A [TaskSelections] map containing the current task data, with any specified override
   *   applied.
   */
  fun getTaskSelections(taskValueOverride: Pair<String, TaskData?>? = null): TaskSelections =
    buildMap {
      _dataState.value.forEach { (task, value) ->
        if (task.id != taskValueOverride?.first) {
          value?.let { put(task.id, it) }
        }
      }

      // Override task value
      taskValueOverride?.let { (taskId, taskValue) -> taskValue?.let { put(taskId, it) } }
    }
}
