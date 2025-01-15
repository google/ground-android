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

import com.google.android.ground.model.task.Task
import com.google.android.ground.model.task.TaskSelections

/**
 * Manages operations related to a sequence of tasks.
 *
 * This class provides methods to retrieve, navigate, and query the position of tasks within a
 * sequence. The sequence is derived from a list of [Task] objects, filtered based on a provided
 * condition.
 *
 * @param tasks The complete list of [Task] objects from which the sequence is derived.
 */
class TaskSequenceHandler(
  private val tasks: List<Task>,
  private val taskDataHandler: TaskDataHandler,
) {

  private var taskSequence: Sequence<Task> = emptySequence()
  private var isSequenceInitialized = false

  init {
    require(tasks.isNotEmpty()) { "Can't generate a sequence from an empty task list." }
  }

  /** Validates the task ID. */
  private fun validateTaskId(taskId: String) {
    require(taskId.isNotBlank()) { "Task ID can't be blank." }
  }

  /** Refreshes the task sequence. */
  fun refreshSequence() {
    taskSequence = generateTaskSequence()
  }

  /** Generates the task sequence based on conditions and overrides. */
  fun generateTaskSequence(taskSelections: TaskSelections? = null): Sequence<Task> {
    val selections = taskSelections ?: taskDataHandler.getTaskSelections()
    return tasks.filter { shouldIncludeTaskInSequence(it, selections) }.asSequence()
  }

  /** Lazily retrieves the task sequence. */
  fun getTaskSequence(): Sequence<Task> {
    if (!isSequenceInitialized) {
      taskSequence = generateTaskSequence()
      isSequenceInitialized = true
    }
    return taskSequence
  }

  /** Determines if a task should be included with the given overrides. */
  private fun shouldIncludeTaskInSequence(task: Task, taskSelections: TaskSelections): Boolean {
    if (task.condition == null) return true
    return task.condition.fulfilledBy(taskSelections)
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
    val index = getTaskIndex(taskId)
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
    val index = getTaskIndex(taskId)
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
    val index = getTaskSequence().indexOfFirst { it.id == taskId }
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
      sequenceSize = getTaskSequence().count(),
    )
}
