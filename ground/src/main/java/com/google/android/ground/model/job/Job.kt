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
package com.google.android.ground.model.job

import android.graphics.Color
import com.google.android.ground.model.task.Task
import timber.log.Timber

/**
 * @param suggestLoiTaskType the type of task used to suggest the LOI for this Job. Null if the job
 * is already associated with an LOI.
 */
data class Job(
  val id: String,
  val style: Style? = null,
  val name: String? = null,
  val tasks: Map<String, Task> = mapOf(),
  val suggestLoiTaskType: Task.Type? = null,
) {
  val tasksSorted: List<Task>
    get() = tasks.values.sortedBy { it.index }

  fun getTask(id: String): Task = tasks[id] ?: error("Unknown task id $id")
}

fun Job.getDefaultColor(): Int =
  try {
    Color.parseColor(style?.color ?: "")
  } catch (e: IllegalArgumentException) {
    Timber.w(e, "Invalid or missing color ${style?.color} in job $id")
    0
  }
