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
  fun getTaskSelections(): TaskSelections {
    return data.mapNotNull { (task, value) -> value?.let { task.id to it } }.toMap()
  }

  fun getData(task: Task): TaskData? {
    return data[task]
  }
}
