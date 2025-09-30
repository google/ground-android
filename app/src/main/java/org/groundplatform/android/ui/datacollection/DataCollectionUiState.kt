/*
 * Copyright 2025 Google LLC
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

import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.task.Task

/**
 * Top-level UI state for the Data Collection flow.
 *
 * This sealed interface models all possible rendering states for the screen and is intended to be
 * the single source of truth for the view layer.
 *
 * Typical transitions:
 * - [Loading] → [Ready] (happy path)
 * - [Loading] → [Error] (boot failures)
 * - [Ready] → [TaskUpdated] → [Ready] (when task sequence/position changes)
 * - [Ready] → [TaskSubmitted] (on successful submission)
 */
sealed interface DataCollectionUiState {

  /**
   * Initial state while dependencies (survey, job, LOI, tasks, drafts) are being resolved. No data
   * is safe to read during this state; the view should show a loading indicator.
   */
  data object Loading : DataCollectionUiState

  /**
   * Fully-resolved, renderable state for the data collection flow.
   *
   * @property surveyId ID of the active survey being answered.
   * @property job The selected job for which tasks are being completed. Stable for the session.
   * @property loiName User-visible name of the Location of Interest (LOI). May be empty when
   *   starting an "Add LOI" flow and filled later.
   * @property tasks Ordered list of tasks to complete. If an LOI is already selected, any "Add LOI"
   *   task is excluded.
   * @property isAddLoiFlow `true` when creating a new LOI (no preselected LOI), `false` when
   *   editing/collecting against an existing LOI.
   * @property currentTaskId ID of the task currently displayed to the user.
   * @property position Metadata for navigation (e.g., index/first/last) for [currentTaskId].
   */
  data class Ready(
    val surveyId: String,
    val job: Job,
    val loiName: String,
    val tasks: List<Task>,
    val isAddLoiFlow: Boolean,
    val currentTaskId: String,
    val position: TaskPosition,
  ) : DataCollectionUiState

  /**
   * Terminal error state for failures that prevent rendering a usable screen.
   *
   * @property code Stable error code to map into localized UI strings.
   * @property cause Optional underlying throwable for logging/diagnostics.
   */
  data class Error(val code: DataCollectionErrorCode, val cause: Throwable? = null) :
    DataCollectionUiState

  /**
   * Ephemeral state emitted when a task update causes navigation or sequence changes (e.g.,
   * next/previous validation alters the current position). The view can react (animate/scroll) and
   * then immediately transition back to [Ready] with the new position.
   *
   * @property position Updated navigation/position metadata after the change.
   */
  data class TaskUpdated(val position: TaskPosition) : DataCollectionUiState

  /**
   * Terminal state indicating that submission succeeded. The view should display a success
   * affordance or navigate away (e.g., back to the home screen) and must not attempt to read or
   * save further draft data for this session.
   */
  data object TaskSubmitted : DataCollectionUiState
}

/** Stable, UI-mappable error codes for data collection bootstrapping and flow. */
enum class DataCollectionErrorCode {
  SURVEY_LOAD_FAILED,
  INVALID_JOB_ID,
  NO_VALID_TASKS,
  INITIAL_TASK_RESOLUTION_FAILED,
  LOI_NAME_COMPUTE_FAILED,
  NETWORK,
  PERMISSION_DENIED,
  IO,
  UNKNOWN,
}

sealed class DataCollectionException(val code: DataCollectionErrorCode, cause: Throwable? = null) :
  Exception(cause) {
  object SurveyLoadFailed : DataCollectionException(DataCollectionErrorCode.SURVEY_LOAD_FAILED)

  object InvalidJobId : DataCollectionException(DataCollectionErrorCode.INVALID_JOB_ID)

  object NoValidTasks : DataCollectionException(DataCollectionErrorCode.NO_VALID_TASKS)

  object LoiNameFailed : DataCollectionException(DataCollectionErrorCode.LOI_NAME_COMPUTE_FAILED)

  class Wrapped(code: DataCollectionErrorCode, cause: Throwable) :
    DataCollectionException(code, cause)
}
