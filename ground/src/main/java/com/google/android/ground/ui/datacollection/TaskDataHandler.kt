package com.google.android.ground.ui.datacollection

import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.Task
import com.google.android.ground.model.task.TaskSelections

class TaskDataHandler(private val taskSequenceHandler: TaskSequenceHandler) {

  private val data: MutableMap<Task, TaskData?> = LinkedHashMap()

  fun setData(key: Task, newValue: TaskData?) {
    val currentValue = data[key]
    if (currentValue == newValue) return
    data[key] = newValue
    taskSequenceHandler.refreshSequence()
  }

  /** Filter deltas to valid tasks. */
  fun getDeltas(): List<ValueDelta> {
    val sequence = taskSequenceHandler.getTaskSequence()
    return data
      .filter { (task) -> task in sequence }
      .map { (task, value) -> ValueDelta(task.id, task.type, value) }
  }

  /** Returns the map of currently selected values. */
  fun getTaskSelections(taskValueOverride: Pair<String, TaskData?>? = null): TaskSelections {
    val selections = data.mapNotNull { (task, value) -> value?.let { task.id to it } }.toMap()
    return taskValueOverride?.let { updateTaskSelections(selections, it) } ?: selections
  }

  /** Updates task selections with overrides. */
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

  fun getData(task: Task): TaskData? {
    return data[task]
  }
}
