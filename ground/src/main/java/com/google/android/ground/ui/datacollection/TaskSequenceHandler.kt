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
import com.google.android.ground.model.task.Task

interface TaskSequenceHandler {

  /**
   * Retrieves the task sequence based on the provided inputs and conditions.
   *
   * This function determines the order of tasks to be presented, taking into account any overrides
   * specified by [taskValueOverride].
   *
   * @param taskValueOverride An optional pair where the first element is the task ID and the second
   *   element is the [TaskData] to override the default task data. If null, no override is applied.
   * @return A [Sequence] of [Task] objects representing the ordered tasks.
   */
  fun getTaskSequence(taskValueOverride: Pair<String, TaskData?>? = null): Sequence<Task>

  /**
   * Checks if the specified task is the first task in the displayed sequence.
   *
   * @param taskId The ID of the task to check.
   * @return `true` if the task is the first in the sequence, `false` otherwise.
   */
  fun isFirstPosition(taskId: String): Boolean

  /**
   * Checks if the specified task is the last task in the displayed sequence.
   *
   * @param taskId The ID of the task to check.
   * @return `true` if the task is the last in the sequence, `false` otherwise.
   */
  fun isLastPosition(taskId: String): Boolean

  /**
   * Checks if the specified task with the given data is the last task in the sequence.
   *
   * This method allows checking if a specific task with a particular [TaskData] is the last one.
   *
   * @param taskId The ID of the task to check.
   * @param value The [TaskData] associated with the task.
   * @return `true` if the task with the given data is the last in the sequence, `false` otherwise.
   */
  fun isLastPosition(taskId: String, value: TaskData?): Boolean

  /**
   * Retrieves the ID of the task that precedes the specified task in the sequence.
   *
   * @param taskId The ID of the task for which to find the previous task.
   * @return The ID of the previous task in the sequence.
   */
  fun getPreviousTask(taskId: String): String

  /**
   * Retrieves the ID of the task that follows the specified task in the sequence.
   *
   * @param taskId The ID of the task for which to find the next task.
   * @return The ID of the next task in the sequence.
   */
  fun getNextTask(taskId: String): String

  /**
   * Retrieves the absolute index of the specified task in the original, unfiltered task sequence.
   *
   * This index represents the task's position in the full sequence, regardless of any filtering or
   * modifications that may have occurred.
   *
   * @param taskId The ID of the task for which to find the absolute position.
   * @throws error if the task is not found in the original task sequence.
   */
  fun getAbsolutePosition(taskId: String): Int

  /**
   * Retrieves the relative index in the computed task sequence.
   *
   * The relative index represents the task's position within the currently displayed sequence.
   *
   * @param taskId The ID of the task for which to find the relative position.
   * @throws error if the task is not found in the computed task sequence.
   */
  fun getRelativePosition(taskId: String): Int

  /**
   * Retrieves the position information for the specified task.
   *
   * @param taskId THE ID of the task for which to find the position information.
   */
  fun getTaskPosition(taskId: String): TaskPosition
}