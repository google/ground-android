package com.google.android.ground.ui.datacollection

import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.Task
import com.google.android.ground.model.task.TaskSelections

class TaskDataHandler(private val taskSequenceHandler: TaskSequenceHandler) {

  private val data: MutableMap<Task, TaskData?> = LinkedHashMap()

  /**
   * Sets the data for a specific task.
   *
   * @param key The task for which to set the data.
   * @param newValue The new data value for the task.
   */
  fun setData(key: Task, newValue: TaskData?) {
    val currentValue = data[key]
    if (currentValue == newValue) return
    data[key] = newValue
    taskSequenceHandler.refreshSequence()
  }

  /**
   * Retrieves the data associated with a specific task.
   *
   * @param task The task to retrieve data for.
   * @return The data associated with the task, or null if no data is found.
   */
  fun getData(task: Task): TaskData? {
    return data[task]
  }

  /** Retrieves a list of [ValueDelta] for tasks that are part of the current sequence. */
  fun getDeltas(): List<ValueDelta> {
    val sequence = taskSequenceHandler.getTaskSequence()
    return data
      .filter { (task) -> task in sequence }
      .map { (task, value) -> ValueDelta(task.id, task.type, value) }
  }

  /**
   * Returns the map of task IDs to their current [TaskData] value.
   *
   * @param taskValueOverride An optional override for a specific task's value.
   */
  fun getTaskSelections(taskValueOverride: Pair<String, TaskData?>? = null): TaskSelections {
    val selections = buildMap { data.forEach { (task, value) -> value?.let { put(task.id, it) } } }
    return taskValueOverride?.let { updateTaskSelections(selections, it) } ?: selections
  }

  /**
   * Updates given task selections with overrides.
   *
   * @param taskSelections The current task selections.
   * @param override The override to apply (task ID and new value).
   */
  private fun updateTaskSelections(
    taskSelections: TaskSelections,
    override: Pair<String, TaskData?>,
  ): TaskSelections {
    val (taskId, taskValue) = override
    return if (taskValue == null) {
      taskSelections.filterNot { it.key == taskId }
    } else {
      taskSelections + (taskId to taskValue)
    }
  }
}
