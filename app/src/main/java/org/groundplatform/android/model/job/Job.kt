/*
 * Copyright 2022 Google LLC
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
package org.groundplatform.android.model.job

import android.graphics.Color
import org.groundplatform.android.model.task.Task
import timber.log.Timber

data class Job(
  val id: String,
  val style: Style? = null,
  val name: String? = null,
  val tasks: Map<String, Task> = mapOf(),
  val strategy: DataCollectionStrategy = DataCollectionStrategy.UNKNOWN,
) {
  enum class DataCollectionStrategy {
    PREDEFINED,
    AD_HOC,
    MIXED,
    UNKNOWN,
  }

  class TaskNotFoundException(taskId: String) : Throwable(message = "unknown task $taskId")

  val canDataCollectorsAddLois: Boolean
    get() = strategy != DataCollectionStrategy.PREDEFINED

  val tasksSorted: List<Task>
    get() = tasks.values.sortedBy { it.index }

  // TODO: Consider using nulls to indicate absence of value here instead of throwing an exception.
  // Issue URL: https://github.com/google/ground-android/issues/2216
  fun getTask(id: String): Task = tasks[id] ?: throw TaskNotFoundException(id)

  /** Job must contain at-most 1 `AddLoiTask`. */
  fun getAddLoiTask(): Task? =
    tasks.values
      .filter { it.isAddLoiTask }
      .apply { check(size <= 1) { "Expected 0 or 1, found $size AddLoiTasks" } }
      .firstOrNull()

  /** Returns whether the job has non-LOI tasks. */
  fun hasNonLoiTasks() = tasks.values.count { !it.isAddLoiTask } > 0
}

fun Job.getDefaultColor(): Int =
  try {
    Color.parseColor(style?.color ?: "")
  } catch (t: Throwable) {
    Timber.w(t, "Invalid or missing color ${style?.color} in job $id")
    Color.BLACK
  }
