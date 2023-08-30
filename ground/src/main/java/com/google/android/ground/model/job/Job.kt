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

import com.google.android.ground.model.task.Task
import java8.util.Optional

/**
 * @param suggestLoiTaskType the type of task used to suggest the LOI for this Job. Null if the job
 * is already associated with an LOI.
 */
data class Job(
  val id: String,
  val style: Style,
  val name: String? = null,
  val tasks: Map<String, Task> = mapOf(),
  val suggestLoiTaskType: Task.Type? = null,
) {
  // TODO(jsunde): Add style and plumb through from firebase
  val tasksSorted: List<Task>
    get() = tasks.values.sortedBy { it.index }
  fun getTask(id: String): Optional<Task> = Optional.ofNullable(tasks[id])
  fun hasData(): Boolean = tasks.isNotEmpty()
}
