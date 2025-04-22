/*
 * Copyright 2024 Google LLC
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

import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.model.task.TaskSelections

/**
 * Manages state and operations related to a sequence of tasks.
 *
 * This class provides methods to retrieve, navigate, and query the position of tasks within a
 * sequence. The sequence is derived from a list of [Task] objects, filtered based on a provided
 * condition.
 *
 * @param allTasks The complete list of [Task] objects from which the final list is derived.
 */
class TaskSequenceHandler(
  private val allTasks: List<Task>,
  private val taskDataHandler: TaskDataHandler,
) {

  private var validTasks: List<Task> = emptyList()
  private var isTaskListReady = false

  init {
    require(allTasks.isNotEmpty()) { "Can't generate a sequence from an empty task list." }
  }

  private fun validateTaskId(taskId: String) {
    require(taskId.isNotBlank()) { "Task ID can't be blank." }
  }

  /** Generates the task sequence based on task's conditions. */
  fun generateValidTasksList(): List<Task> {
    val selections = taskDataHandler.getTaskSelections()
    return allTasks.filter { it.isConditionFulfilled(selections) }
  }

  /** Returns true if the specified task would be last in the sequence with the given value. */
  fun checkIfTaskIsLastWithValue(taskValueOverride: Pair<String, TaskData?>): Boolean {
    val overriddenTaskId = taskValueOverride.first
    validateTaskId(overriddenTaskId)

    val selections = taskDataHandler.getTaskSelections(taskValueOverride)
    val lastTask = allTasks.last { it.isConditionFulfilled(selections) }
    return lastTask.id == overriddenTaskId
  }

  /** Determines if a task's conditions are fulfilled using the given [taskSelections]. */
  private fun Task.isConditionFulfilled(taskSelections: TaskSelections): Boolean =
    condition == null || condition.fulfilledBy(taskSelections)

  /** Returns the pre-computed list of valid tasks or generates/caches it first. */
  fun getValidTasks(): List<Task> {
    if (!isTaskListReady) {
      validTasks = generateValidTasksList()
      isTaskListReady = true
    }
    return validTasks
  }

  /** Resets the local cache of the task list. */
  fun invalidateCache() {
    isTaskListReady = false
    validTasks = emptyList()
  }

  /**
   * Checks if the specified task is the first task in the displayed tasks list.
   *
   * @param taskId The ID of the task to check.
   * @return `true` if the task is the first in the list, `false` otherwise.
   * @throws IllegalArgumentException if the provided [taskId] is blank.
   */
  fun isFirstPosition(taskId: String): Boolean {
    validateTaskId(taskId)
    return taskId == getValidTasks().first().id
  }

  /**
   * Checks if the specified task is the last task in the displayed tasks list.
   *
   * @param taskId The ID of the task to check.
   * @return `true` if the task is the last in the list, `false` otherwise.
   * @throws IllegalArgumentException if the provided [taskId] is blank.
   */
  fun isLastPosition(taskId: String): Boolean {
    validateTaskId(taskId)
    return taskId == getValidTasks().last().id
  }

  /**
   * Retrieves the ID of the task that precedes the specified task in the sequence.
   *
   * @param taskId The ID of the task for which to find the previous task.
   * @return The ID of the previous task in the sequence.
   * @throws IllegalArgumentException if the provided [taskId] is blank.
   * @throws NoSuchElementException if the provided [taskId] is the first element in the sequence.
   */
  fun getPreviousTask(taskId: String): String {
    val index = getTaskIndex(taskId)
    require(index > 0) { "Can't generate previous task for Task '$taskId'" }
    return getValidTasks().elementAt(index - 1).id
  }

  /**
   * Retrieves the ID of the task that follows the specified task in the sequence.
   *
   * @param taskId The ID of the task for which to find the next task.
   * @return The ID of the next task in the sequence.
   * @throws IllegalArgumentException if the provided [taskId] is blank.
   * @throws NoSuchElementException if the provided [taskId] is the last element in the sequence.
   */
  fun getNextTask(taskId: String): String {
    val index = getTaskIndex(taskId)
    require(index + 1 < getValidTasks().count()) { "Can't generate next task for Task '$taskId'" }
    return getValidTasks().elementAt(index + 1).id
  }

  /**
   * Retrieves the absolute index of the specified task in the original, unfiltered task sequence.
   *
   * This index represents the task's position in the full sequence, regardless of any filtering or
   * modifications that may have occurred.
   *
   * @param taskId The ID of the task for which to find the absolute position.
   * @throws IllegalArgumentException if the provided [taskId] is blank.
   * @throws NoSuchElementException if the task is not found in the original task sequence.
   */
  fun getAbsolutePosition(taskId: String): Int {
    validateTaskId(taskId)
    val index = allTasks.indexOfFirst { it.id == taskId }
    require(index >= 0) { "Task '$taskId' not found in the task list." }
    return index
  }

  /**
   * Retrieves the relative index of task in the computed task sequence.
   *
   * The relative index represents the task's position within the currently displayed sequence.
   *
   * @param taskId The ID of the task for which to find the relative position.
   * @throws IllegalArgumentException if the provided [taskId] is blank.
   * @throws NoSuchElementException if the task is not found in the computed task sequence.
   */
  fun getTaskIndex(taskId: String): Int {
    validateTaskId(taskId)
    val index = getValidTasks().indexOfFirst { it.id == taskId }
    require(index >= 0) { "Task '$taskId' not found in the sequence." }
    return index
  }

  /**
   * Retrieves the position information for the specified task.
   *
   * @param taskId The ID of the task for which to find the position information.
   * @throws IllegalArgumentException if the provided [taskId] is blank.
   * @throws NoSuchElementException if the task is not found in the computed task sequence.
   */
  fun getTaskPosition(taskId: String): TaskPosition =
    TaskPosition(
      absoluteIndex = getAbsolutePosition(taskId),
      relativeIndex = getTaskIndex(taskId),
      sequenceSize = getValidTasks().count(),
    )
}
