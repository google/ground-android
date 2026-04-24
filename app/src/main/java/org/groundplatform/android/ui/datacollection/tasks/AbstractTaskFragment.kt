/*
 * Copyright 2023 Google LLC
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

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.datacollection.DataCollectionUiState
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction

abstract class AbstractTaskFragment<T : AbstractTaskViewModel> : AbstractFragment() {

  protected val dataCollectionViewModel: DataCollectionViewModel by
    hiltNavGraphViewModels(R.id.data_collection)

  protected val taskId: String by lazy {
    arguments?.getString(TASK_ID) ?: error("taskId not found in arguments")
  }

  protected val viewModel: T by lazy {
    @Suppress("UNCHECKED_CAST")
    dataCollectionViewModel.getTaskViewModel(taskId) as? T
      ?: error("ViewModel for taskId:$taskId not found.")
  }

  private fun onSkip() {
    check(viewModel.hasNoData()) { "User should not be able to skip a task with data." }
    viewModel.setSkipped()
    moveToNext()
  }

  private fun moveToPrevious() {
    dataCollectionViewModel.onPreviousClicked(viewModel)
  }

  private fun moveToNext() {
    dataCollectionViewModel.onNextClicked(viewModel)
  }

  private fun handleNext() {
    if (viewModel.task.isAddLoiTask) {
      dataCollectionViewModel.openLoiNameDialog()
    } else {
      moveToNext()
    }
  }

  /**
   * Updates the [DataCollectionViewModel] with the current vertical position of the task footer.
   * Only updates if this task is the currently active (visible) task, preventing off-screen
   * preloaded pages from overwriting the position.
   */
  fun saveFooterPosition(top: Float) {
    val isActive =
      (dataCollectionViewModel.uiState.value as? DataCollectionUiState.Ready)?.currentTaskId ==
        taskId
    if (isActive) {
      dataCollectionViewModel.updateFooterPosition(top)
    }
  }

  /** Handles actions triggered from the task screen UI. */
  fun handleTaskScreenAction(screenAction: TaskScreenAction) {
    when (screenAction) {
      is TaskScreenAction.OnButtonClicked -> {
        handleButtonClick(screenAction.action)
      }
      is TaskScreenAction.OnLoiNameChanged -> {
        dataCollectionViewModel.setLoiNameDraft(screenAction.name)
      }
      is TaskScreenAction.OnLoiNameDismiss -> {
        dataCollectionViewModel.dismissLoiNameDialog(dataCollectionViewModel.getLoiName())
      }
      is TaskScreenAction.OnLoiNameConfirm -> {
        dataCollectionViewModel.confirmLoiName(screenAction.name)
        moveToNext()
      }
      is TaskScreenAction.OnInstructionsDismiss -> {
        error("OnInstructionsDismiss action should be handled within the task screen")
      }
    }
  }

  private fun handleButtonClick(action: ButtonAction) {
    when (action) {
      // Navigation actions
      ButtonAction.PREVIOUS -> moveToPrevious()
      ButtonAction.NEXT,
      ButtonAction.DONE -> handleNext()
      ButtonAction.SKIP -> onSkip()
      // Task-specific actions - delegate to ViewModel
      else -> viewModel.onButtonClick(action)
    }
  }

  companion object {
    const val TASK_ID = "taskId"
  }
}
