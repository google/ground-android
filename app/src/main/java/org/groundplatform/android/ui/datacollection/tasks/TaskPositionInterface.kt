/*
 * Copyright 2026 Google LLC
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

import org.groundplatform.android.model.submission.TaskData

/**
 * Interface for querying the position of a task within a sequence.
 *
 * This interface is used to delegate task sequencing knowledge to task-specific ViewModels. It
 * allows the receiving ViewModel to make UI-related decisions (e.g. showing the correct action
 * buttons) without requiring direct access to the full task list or sequencing logic.
 */
interface TaskPositionInterface {
  /** Returns true if the task is the first in the sequence. */
  fun isFirst(): Boolean

  /**
   * Returns true if the current task is the final task in the flow, taking into account any
   * conditional sequencing based on [taskData].
   *
   * For example, some tasks may be skipped or added depending on user responses, which can affect
   * whether this task is considered last.
   */
  fun isLastWithValue(taskData: TaskData?): Boolean
}
