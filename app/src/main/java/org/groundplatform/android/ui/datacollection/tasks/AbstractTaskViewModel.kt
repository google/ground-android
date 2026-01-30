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
package org.groundplatform.android.ui.datacollection.tasks

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.groundplatform.android.R
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.SkippedTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.isNotNullOrEmpty
import org.groundplatform.android.model.submission.isNullOrEmpty
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.refactor.ButtonActionState

/** Defines the state of an inflated [Task] and controls its UI. */
abstract class AbstractTaskViewModel internal constructor() : AbstractViewModel() {

  /** Current value. */
  private val _taskDataFlow: MutableStateFlow<TaskData?> = MutableStateFlow(null)
  val taskTaskData: StateFlow<TaskData?> = _taskDataFlow.asStateFlow()

  abstract val taskActionButtonStates: StateFlow<List<ButtonActionState>>

  lateinit var task: Task
  private lateinit var isFirstPosition: () -> Boolean
  private lateinit var isLastPositionWithValue: (TaskData?) -> Boolean

  open fun initialize(
    job: Job,
    task: Task,
    taskData: TaskData?,
    isFirstPosition: () -> Boolean,
    isLastPosition: (TaskData?) -> Boolean,
  ) {
    this.task = task
    this.isFirstPosition = isFirstPosition
    this.isLastPositionWithValue = isLastPosition
    setValue(taskData)
  }

  /** Checks if the current value is valid and updates error value. */
  fun validate(): Int? = validate(task, taskTaskData.value)

  /**
   * Performs input validation on the given [Task] and associated [TaskData].
   *
   * Returns an [Int] identifier for an error string if validation fails, returns null otherwise.
   * Subclasses may override this method to validate input data and display an error message to the
   * user.
   */
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

  fun getPreviousButtonState(): ButtonActionState =
    ButtonActionState(
      action = ButtonAction.PREVIOUS,
      isEnabled = !isFirstPosition(),
      isVisible = true,
    )

  fun getNextButtonState(taskData: TaskData?, hideIfEmpty: Boolean = false): ButtonActionState {
    val isVisible = if (hideIfEmpty) taskData.isNotNullOrEmpty() else true
    return if (isLastPositionWithValue(taskData)) {
      ButtonActionState(
        action = ButtonAction.DONE,
        isEnabled = taskData.isNotNullOrEmpty(),
        isVisible = isVisible,
      )
    } else {
      ButtonActionState(
        action = ButtonAction.NEXT,
        isEnabled = taskData.isNotNullOrEmpty(),
        isVisible = isVisible,
      )
    }
  }

  fun getSkipButtonState(taskData: TaskData?): ButtonActionState =
    ButtonActionState(
      action = ButtonAction.SKIP,
      isEnabled = isTaskOptional(),
      isVisible = isTaskOptional() && taskData.isNullOrEmpty(),
    )

  fun getUndoButtonState(taskData: TaskData?): ButtonActionState =
    ButtonActionState(
      action = ButtonAction.UNDO,
      isEnabled = taskData.isNotNullOrEmpty(),
      isVisible = taskData.isNotNullOrEmpty(),
    )

  open fun onButtonClick(action: ButtonAction) {
    if (action == ButtonAction.UNDO) {
      clearResponse()
    } else {
      // Subclasses handle other actions
    }
  }
}
