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
package org.groundplatform.android.ui.datacollection

import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.task.Task

sealed interface DataCollectionUiState {
  data object Loading : DataCollectionUiState

  data class Ready(
    val surveyId: String,
    val job: Job,
    val tasks: List<Task>,
    val isAddLoiFlow: Boolean,
    val currentTaskId: String,
    val position: TaskPosition,
  ) : DataCollectionUiState

  data class Error(val message: String) : DataCollectionUiState

  data class TaskUpdated(val position: TaskPosition) : DataCollectionUiState

  data object TaskSubmitted : DataCollectionUiState
}
