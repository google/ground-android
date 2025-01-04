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

class TaskSequenceHandlerImpl(
  private val tasks: List<Task>,
  private val shouldIncludeTask:
    (task: Task, taskValueOverride: Pair<String, TaskData?>?) -> Boolean,
) : TaskSequenceHandler {

  init {
    if (tasks.isEmpty()) {
      error("Can't generate sequence for empty task list")
    }
  }

  private fun checkInvalidTaskId(taskId: String) {
    if (taskId.isBlank()) {
      error("Task ID can't be blank")
    }
  }

  private fun checkInvalidIndex(taskId: String, index: Int) {
    checkInvalidTaskId(taskId)
    if (index < 0) {
      error("Task $taskId not found in task list")
    }
  }

  override fun getTaskSequence(taskValueOverride: Pair<String, TaskData?>?): Sequence<Task> =
    tasks.filter { task -> shouldIncludeTask(task, taskValueOverride) }.asSequence()

  override fun isFirstPosition(taskId: String): Boolean {
    checkInvalidTaskId(taskId)
    return taskId == getTaskSequence().first().id
  }

  override fun isLastPosition(taskId: String): Boolean {
    checkInvalidTaskId(taskId)
    return taskId == getTaskSequence().last().id
  }

  override fun isLastPosition(taskId: String, value: TaskData?): Boolean {
    checkInvalidTaskId(taskId)
    return taskId == getTaskSequence(taskValueOverride = taskId to value).last().id
  }

  override fun getPreviousTask(taskId: String): String {
    checkInvalidTaskId(taskId)
    return getTaskSequence().elementAt(getRelativePosition(taskId) - 1).id
  }

  override fun getNextTask(taskId: String): String {
    checkInvalidTaskId(taskId)
    return getTaskSequence().elementAt(getRelativePosition(taskId) + 1).id
  }

  override fun getAbsolutePosition(taskId: String): Int {
    val index = tasks.indexOfFirst { it.id == taskId }
    checkInvalidIndex(taskId, index)
    return index
  }

  override fun getRelativePosition(taskId: String): Int {
    val index = getTaskSequence().indexOfFirst { it.id == taskId }
    checkInvalidIndex(taskId, index)
    return index
  }

  override fun getTaskPosition(taskId: String): TaskPosition {
    checkInvalidTaskId(taskId)
    return TaskPosition(
      absoluteIndex = getAbsolutePosition(taskId),
      relativeIndex = getRelativePosition(taskId),
      sequenceSize = getTaskSequence().toList().size,
    )
  }
}
