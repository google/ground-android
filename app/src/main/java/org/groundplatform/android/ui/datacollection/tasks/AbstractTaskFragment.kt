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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.properties.Delegates
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.datacollection.DataCollectionUiState
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.InstructionsDialog
import org.groundplatform.android.ui.datacollection.components.LoiNameDialog
import org.groundplatform.android.ui.datacollection.components.TaskFooter
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.components.TaskViewLayout
import org.groundplatform.android.util.createComposeView
import org.groundplatform.domain.model.task.Task

abstract class AbstractTaskFragment<T : AbstractTaskViewModel> : AbstractFragment() {

  protected val dataCollectionViewModel: DataCollectionViewModel by
    hiltNavGraphViewModels(R.id.data_collection)

  /** ID of the associated task in the Job. Used for instantiating the [viewModel]. */
  var taskId by Delegates.notNull<String>()

  protected val viewModel: T by lazy {
    @Suppress("UNCHECKED_CAST")
    dataCollectionViewModel.getTaskViewModel(taskId) as? T
      ?: error("ViewModel for taskId:$taskId not found.")
  }

  /** Represents the content to be shown in the task header, if any. */
  open val taskHeader: TaskHeader? by lazy {
    TaskHeader(label = viewModel.task.label, iconResId = R.drawable.ic_question_answer)
  }

  /** Represents the content to be shown in the task instructions, if any. */
  open val instructionData: InstructionData? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      taskId = requireNotNull(savedInstanceState.getString(TASK_ID))
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(TASK_ID, taskId)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = createComposeView {
    TaskViewLayout(header = taskHeader, footer = { TaskFooter() }, content = { TaskBody() })

    if (getTask().isAddLoiTask) {
      LoiNameDialog()
    }

    instructionData?.let { InstructionsDialog(it) }
  }

  override fun onResume() {
    super.onResume()
    onTaskResume()
  }

  /** Renders the body of the task. */
  @Composable
  open fun TaskBody() {
    // This method should be moved within the respective compose screen. Remove once all tasks have
    // been migrated to compose layout using [TaskScreen].
  }

  /** Invoked when the instruction dialog is dismissed. */
  open fun onInstructionDialogDismissed() {}

  /** Invoked when the task fragment is visible to the user. */
  open fun onTaskResume() {}

  private fun onSkip() {
    check(viewModel.hasNoData()) { "User should not be able to skip a task with data." }
    viewModel.setSkipped()
    moveToNext()
  }

  private fun moveToPrevious() {
    dataCollectionViewModel.onPreviousClicked(viewModel)
  }

  fun moveToNext() {
    dataCollectionViewModel.onNextClicked(viewModel)
  }

  private fun handleNext() {
    if (getTask().isAddLoiTask) {
      dataCollectionViewModel.loiNameDialogOpen.value = true
    } else {
      moveToNext()
    }
  }

  private fun handleLoiNameSet(loiName: String) {
    if (loiName != "") {
      dataCollectionViewModel.setLoiName(loiName)
      moveToNext()
    }
  }

  /** Adds the action buttons to the UI. */
  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  internal fun TaskFooter() {
    val isKeyboardOpen = WindowInsets.isImeVisible
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()

    // Update footer position whenever layout changes or keyboard is toggled.
    LaunchedEffect(isKeyboardOpen, layoutCoordinates) {
      layoutCoordinates?.let { saveFooterPosition(it.positionInWindow().y) }
    }

    TaskFooter(
      modifier = Modifier.onGloballyPositioned { layoutCoordinates = it },
      headerCard =
        if (shouldShowHeader()) {
          { HeaderCard() }
        } else {
          null
        },
      buttonActionStates = taskActionButtonsStates,
      onButtonClicked = { handleButtonClick(it) },
    )
  }

  /**
   * Updates the [DataCollectionViewModel] with the current vertical position of the task footer.
   * Only updates if this task is the currently active (visible) task, preventing off-screen
   * preloaded pages from overwriting the position.
   */
  fun saveFooterPosition(top: Float) {
    val isActive =
      (dataCollectionViewModel.uiState?.value as? DataCollectionUiState.Ready)?.currentTaskId ==
        taskId
    if (isActive) {
      dataCollectionViewModel.updateFooterPosition(top)
    }
  }

  /** Handles actions triggered from the task screen UI. */
  fun handleTaskScreenAction(screenAction: TaskScreenAction) {
    if (screenAction is TaskScreenAction.OnButtonClicked) {
      handleButtonClick(screenAction.action)
    } else {
      // TODO: Handle other actions
      // https://github.com/google/ground-android/issues/3630
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

  // This function can allow any task to show a Header card on top of the Button row.
  open fun shouldShowHeader() = false

  @Composable open fun HeaderCard() {}

  private fun getTask(): Task = viewModel.task

  @Composable
  private fun LoiNameDialog() {
    var openAlertDialog by dataCollectionViewModel.loiNameDialogOpen

    if (openAlertDialog) {
      val uiState by dataCollectionViewModel.uiState.collectAsStateWithLifecycle()
      val initialNameValue =
        (uiState as? DataCollectionUiState.Ready)?.loiName
          ?: dataCollectionViewModel.getTypedLoiNameOrEmpty()
      var name by rememberSaveable(initialNameValue) { mutableStateOf(initialNameValue) }

      LoiNameDialog(
        textFieldValue = name,
        onConfirmRequest = {
          openAlertDialog = false
          handleLoiNameSet(name)
        },
        onDismissRequest = {
          name = initialNameValue
          openAlertDialog = false
        },
        onTextFieldChange = { name = it },
      )
    }
  }

  @Composable
  private fun InstructionsDialog(instructionData: InstructionData) {
    var showInstructionsDialog by viewModel.showInstructionsDialog

    if (showInstructionsDialog) {
      InstructionsDialog(
        data = instructionData,
        onDismissed = {
          showInstructionsDialog = false
          onInstructionDialogDismissed()
        },
      )
    }
  }

  companion object {
    const val TASK_ID = "taskId"
  }
}
