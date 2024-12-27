/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.ui.datacollection.tasks

import com.google.android.ground.R
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.SkippedTaskData
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.AbstractViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Defines the state of an inflated [Task] and controls its UI. */
open class AbstractTaskViewModel internal constructor() : AbstractViewModel() {

  /** Current value. */
  private val _taskDataFlow: MutableStateFlow<TaskData?> = MutableStateFlow(null)
  val taskTaskData: StateFlow<TaskData?> = _taskDataFlow.asStateFlow()

  lateinit var task: Task

  open fun initialize(job: Job, task: Task, taskData: TaskData?) {
    this.task = task
    setValue(taskData)
  }

  /** Checks if the current value is valid and updates error value. */
  fun validate(): Int? = validate(task, taskTaskData.value)

  open fun validate(task: Task, taskData: TaskData?): Int? {
    // Empty response for a required task.
    if (task.isRequired && (taskData == null || taskData.isEmpty())) {
      return R.string.required_task
    }
    return null
  }

  fun setSkipped() {
    setValue(SkippedTaskData())
  }

  fun setValue(taskData: TaskData?) {
    _taskDataFlow.update { taskData }
  }

  open fun clearResponse() {
    setValue(null)
  }

  fun isTaskOptional(): Boolean = !task.isRequired

  fun hasNoData(): Boolean = taskTaskData.value.isNullOrEmpty()
}
