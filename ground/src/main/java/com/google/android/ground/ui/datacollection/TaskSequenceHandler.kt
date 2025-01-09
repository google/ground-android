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
package com.google.android.ground.ui.datacollection

import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.task.Condition
import com.google.android.ground.model.task.Task
import com.google.android.ground.model.task.TaskSelections
import timber.log.Timber

/**
 * Manages operations related to a sequence of tasks.
 *
 * This class provides methods to retrieve, navigate, and query the position of tasks within a
 * sequence. The sequence is derived from a list of [Task] objects, filtered based on a provided
 * condition.
 *
 * @param tasks The complete list of [Task] objects from which the sequence is derived.
 */
class TaskSequenceHandler(private val tasks: List<Task>) {

  val taskDataHandler = TaskDataHandler(this)

  private var sequence: Sequence<Task> = emptySequence()
  private var isInitialized = false

  init {
    require(tasks.isNotEmpty()) { "Can't generate a sequence from an empty task list." }
  }

  /** Validates the task ID. */
  private fun validateTaskId(taskId: String) {
    require(taskId.isNotBlank()) { "Task ID can't be blank." }
  }

  /** Refreshes the task sequence. */
  fun refreshSequence() {
    sequence = generateTaskSequence("refreshSequence")
  }

  /** Generates the task sequence based on conditions and overrides. */
  private fun generateTaskSequence(
    tag: String,
    taskValueOverride: Pair<String, TaskData?>? = null,
  ): Sequence<Task> {
    Timber.d("Task Sequence Generated: $tag")
    return tasks
      .filter { task ->
        task.condition == null ||
          isConditionMet(task.condition, taskDataHandler.getTaskSelections(), taskValueOverride)
      }
      .asSequence()
  }

  /** Lazily retrieves the task sequence. */
  fun getTaskSequence(): Sequence<Task> {
    if (!isInitialized) {
      sequence = generateTaskSequence("getTaskSequence")
      isInitialized = true
    }
    return sequence
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

  /** Determines if a condition is met with the given task selections and overrides. */
  private fun isConditionMet(
    condition: Condition,
    taskSelections: TaskSelections,
    taskValueOverride: Pair<String, TaskData?>? = null,
  ): Boolean {
    val updatedSelections =
      taskValueOverride?.let { updateTaskSelections(taskSelections, it) } ?: taskSelections
    return condition.fulfilledBy(updatedSelections.toMap())
  }

  /**
   * Checks if the specified task is the first task in the displayed sequence.
   *
   * @param taskId The ID of the task to check.
   * @return `true` if the task is the first in the sequence, `false` otherwise.
   * @throws IllegalArgumentException if the provided [taskId] is blank.
   */
  fun isFirstPosition(taskId: String): Boolean {
    validateTaskId(taskId)
    return taskId == getTaskSequence().first().id
  }

  /**
   * Checks if the specified task is the last task in the displayed sequence.
   *
   * @param taskId The ID of the task to check.
   * @return `true` if the task is the last in the sequence, `false` otherwise.
   * @throws IllegalArgumentException if the provided [taskId] is blank.
   */
  fun isLastPosition(taskId: String): Boolean {
    validateTaskId(taskId)
    return taskId == getTaskSequence().last().id
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
    validateTaskId(taskId)
    val index = getTaskIndex(taskId, getTaskSequence())
    require(index > 0) { "Can't generate previous task for Task '$taskId'" }
    return getTaskSequence().elementAt(index - 1).id
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
    validateTaskId(taskId)
    val index = getTaskIndex(taskId, getTaskSequence())
    require(index < getTaskSequence().count()) { "Can't generate next task for Task '$taskId'" }
    return getTaskSequence().elementAt(index + 1).id
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
    val index = tasks.indexOfFirst { it.id == taskId }
    require(index >= 0) { "Task '$taskId' not found in the task list." }
    return index
  }

  /** Retrieves the relative position of a task in the sequence. */
  fun getTaskIndex(taskId: String, sequence: Sequence<Task>): Int {
    val index = sequence.indexOfFirst { it.id == taskId }
    require(index >= 0) { "Task '$taskId' not found in the sequence." }
    return index
  }

  /**
   * Retrieves the relative index in the computed task sequence.
   *
   * The relative index represents the task's position within the currently displayed sequence.
   *
   * @param taskId The ID of the task for which to find the relative position.
   * @throws IllegalArgumentException if the provided [taskId] is blank.
   * @throws NoSuchElementException if the task is not found in the computed task sequence.
   */
  fun getRelativePosition(taskId: String): Int {
    validateTaskId(taskId)
    return getTaskIndex(taskId, getTaskSequence())
  }

  /**
   * Retrieves the position information for the specified task.
   *
   * @param taskId The ID of the task for which to find the position information.
   * @throws IllegalArgumentException if the provided [taskId] is blank.
   * @throws NoSuchElementException if the task is not found in the computed task sequence.
   */
  fun getTaskPosition(taskId: String): TaskPosition {
    validateTaskId(taskId)
    return TaskPosition(
      absoluteIndex = getAbsolutePosition(taskId),
      relativeIndex = getRelativePosition(taskId),
      sequenceSize = getTaskSequence().count(),
    )
  }
}
