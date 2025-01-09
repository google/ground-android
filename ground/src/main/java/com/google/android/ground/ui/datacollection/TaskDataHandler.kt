package com.google.android.ground.ui.datacollection

import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.Condition
import com.google.android.ground.model.task.Task

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

  /**
   * Function that determines whether a given [Condition] is fulfilled by the currently set values
   * for the tasks. current set of [TaskData].
   *
   * A callback function that determines whether a given [Task] should be included in the sequence.
   * It takes a [Task] and an optional override pair as input and returns `true` if the task should
   * be included, `false` otherwise.
   */
  fun isConditionFulfilled(
    condition: Condition,
    taskValueOverride: Pair<String, TaskData?>? = null,
  ): Boolean {
    val pairs = data.mapNotNull { (task, value) -> value?.let { task.id to it } }
    val updatedPairs =
      if (taskValueOverride != null) {
        val (taskId, taskValue) = taskValueOverride
        if (taskValue == null) {
          // Remove pairs with the taskId if value is null
          pairs.filterNot { it.first == taskId }
        } else {
          // Override any task IDs with the value
          pairs + (taskId to taskValue)
        }
      } else {
        pairs
      }
    return condition.fulfilledBy(updatedPairs.toMap())
  }

  fun getData(task: Task): TaskData? {
    return data[task]
  }
}
