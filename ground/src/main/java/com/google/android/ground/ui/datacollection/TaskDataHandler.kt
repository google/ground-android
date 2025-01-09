package com.google.android.ground.ui.datacollection

import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.task.Task
import com.google.android.ground.model.task.TaskSelections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TaskDataHandler {

  private val _dataState = MutableStateFlow(LinkedHashMap<Task, TaskData?>())

  val dataState: StateFlow<Map<Task, TaskData?>>
    get() = _dataState.asStateFlow()

  /**
   * Sets the data for a specific task.
   *
   * @param key The task for which to set the data.
   * @param newValue The new data value for the task.
   */
  fun setData(key: Task, newValue: TaskData?) {
    if (getData(key) == newValue) return
    // Ensure that the map is recreated to ensure that the state flow is emitted.
    val data = LinkedHashMap(_dataState.value).apply { set(key, newValue) }
    _dataState.value = data
  }

  /**
   * Retrieves the data associated with a specific task.
   *
   * @param task The task to retrieve data for.
   * @return The data associated with the task, or null if no data is found.
   */
  fun getData(task: Task): TaskData? {
    return _dataState.value[task]
  }

  /**
   * Returns the map of task IDs to their current [TaskData] value.
   *
   * @param taskValueOverride An optional override for a specific task's value.
   */
  fun getTaskSelections(taskValueOverride: Pair<String, TaskData?>? = null): TaskSelections =
    buildMap {
      _dataState.value.forEach { (task, value) ->
        if (taskValueOverride?.first == task.id) {
            taskValueOverride.second
          } else {
            value
          }
          ?.apply { put(task.id, this) }
      }
    }
}
