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
package org.groundplatform.android.ui.datacollection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskViewModel

/** View model for the Data Collection fragment. */
@HiltViewModel
class DataCollectionViewModel
@Inject
internal constructor(private val helper: DataCollectionHelper, savedStateHandle: SavedStateHandle) :
  AbstractViewModel() {

  val uiState: StateFlow<DataCollectionUiState>
    get() = helper.uiState

  val loiNameDialogOpen = helper.loiNameDialogOpen

  init {
    helper.initialize(savedStateHandle, viewModelScope)
  }

  fun getTaskViewModel(taskId: String) = helper.getTaskViewModel(taskId)

  fun onNextClicked(taskVm: AbstractTaskViewModel) = helper.onNextClicked(taskVm)

  fun onPreviousClicked(taskVm: AbstractTaskViewModel) = helper.onPreviousClicked(taskVm)

  fun saveCurrentState() = helper.saveCurrentState()

  fun clearDraftBlocking() = helper.clearDraftBlocking()

  fun isFirstPosition(taskId: String) = helper.isFirstPosition(taskId)

  fun isLastPosition(taskId: String) = helper.isLastPosition(taskId)

  fun requireSurveyId(): String = helper.requireSurveyId()

  fun isAtFirstTask(): Boolean = helper.isAtFirstTask()

  fun setLoiName(name: String) = helper.setLoiName(name)

  fun isLastPositionWithValue(task: Task, value: TaskData?) =
    helper.isLastPositionWithValue(task, value)

  fun moveToPreviousTask() = helper.moveToPreviousTask()
}
