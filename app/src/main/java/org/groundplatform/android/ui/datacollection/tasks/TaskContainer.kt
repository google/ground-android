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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.datacollection.DataCollectionUiState
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.InstructionsDialog
import org.groundplatform.android.ui.datacollection.components.LoiNameDialog
import org.groundplatform.android.ui.datacollection.components.TaskFooter
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.components.TaskViewLayout

@Composable
fun TaskContainer(
  viewModel: AbstractTaskViewModel,
  dataCollectionViewModel: DataCollectionViewModel,
  taskHeader: TaskHeader? =
    TaskHeader(label = viewModel.task.label, iconResId = R.drawable.ic_question_answer),
  instructionData: InstructionData? = null,
  shouldShowHeader: Boolean = false,
  headerCard: @Composable (() -> Unit)? = null,
  onInstructionDialogDismissed: () -> Unit = {},
  content: @Composable () -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val uiState by dataCollectionViewModel.uiState.collectAsStateWithLifecycle()

  val initialNameValue =
    (uiState as? DataCollectionUiState.Ready)?.loiName
      ?: dataCollectionViewModel.getTypedLoiNameOrEmpty()

  fun handleNext() {
    if (viewModel.task.isAddLoiTask) {
      dataCollectionViewModel.loiNameDialogOpen.value = true
    } else {
      dataCollectionViewModel.onNextClicked(viewModel)
    }
  }

  fun handleButtonClick(action: ButtonAction) {
    when (action) {
      ButtonAction.PREVIOUS -> dataCollectionViewModel.onPreviousClicked(viewModel)
      ButtonAction.NEXT,
      ButtonAction.DONE -> handleNext()
      ButtonAction.SKIP -> {
        check(viewModel.hasNoData()) { "User should not be able to skip a task with data." }
        viewModel.setSkipped()
        dataCollectionViewModel.onNextClicked(viewModel)
      }
      else -> viewModel.onButtonClick(action)
    }
  }

  fun handleLoiNameConfirm(name: String) {
    dataCollectionViewModel.loiNameDialogOpen.value = false
    if (name.isNotBlank()) {
      dataCollectionViewModel.setLoiName(name)
      dataCollectionViewModel.onNextClicked(viewModel)
    }
  }

  fun handleInstructionsDismiss() {
    viewModel.showInstructionsDialog.value = false
    onInstructionDialogDismissed()
  }

  TaskContainerUi(
    taskHeader = taskHeader,
    instructionData = instructionData,
    shouldShowHeader = shouldShowHeader,
    headerCard = headerCard,
    taskActionButtonsStates = taskActionButtonsStates,
    isAddLoiTask = viewModel.task.isAddLoiTask,
    loiNameDialogOpen = dataCollectionViewModel.loiNameDialogOpen.value,
    initialNameValue = initialNameValue,
    showInstructionsDialog = viewModel.showInstructionsDialog.value,
    onFooterPositionUpdated = { dataCollectionViewModel.updateFooterPosition(it) },
    onButtonClicked = ::handleButtonClick,
    onLoiNameConfirm = ::handleLoiNameConfirm,
    onLoiNameDismiss = { dataCollectionViewModel.loiNameDialogOpen.value = false },
    onInstructionsDismiss = ::handleInstructionsDismiss,
    content = content,
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskContainerUi(
  taskHeader: TaskHeader?,
  instructionData: InstructionData?,
  shouldShowHeader: Boolean,
  headerCard: @Composable (() -> Unit)?,
  taskActionButtonsStates: List<ButtonActionState>,
  isAddLoiTask: Boolean,
  loiNameDialogOpen: Boolean,
  initialNameValue: String,
  showInstructionsDialog: Boolean,
  onFooterPositionUpdated: (Float) -> Unit,
  onButtonClicked: (ButtonAction) -> Unit,
  onLoiNameConfirm: (String) -> Unit,
  onLoiNameDismiss: () -> Unit,
  onInstructionsDismiss: () -> Unit,
  content: @Composable () -> Unit,
) {
  val isKeyboardOpen = WindowInsets.isImeVisible
  var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

  // Update footer position whenever layout changes or keyboard is toggled.
  LaunchedEffect(isKeyboardOpen, layoutCoordinates) {
    layoutCoordinates?.let { onFooterPositionUpdated(it.positionInWindow().y) }
  }

  TaskViewLayout(
    header = taskHeader,
    footer = {
      TaskFooter(
        modifier = Modifier.onGloballyPositioned { layoutCoordinates = it },
        headerCard = headerCard.takeIf { shouldShowHeader },
        buttonActionStates = taskActionButtonsStates,
        onButtonClicked = onButtonClicked,
      )
    },
    content = content,
  )

  if (isAddLoiTask && loiNameDialogOpen) {
    val nameState = rememberSaveable { mutableStateOf(initialNameValue) }

    LoiNameDialog(
      textFieldValue = nameState.value,
      onConfirmRequest = { onLoiNameConfirm(nameState.value) },
      onDismissRequest = {
        nameState.value = initialNameValue
        onLoiNameDismiss()
      },
      onTextFieldChange = { nameState.value = it },
    )
  }

  instructionData
    ?.takeIf { showInstructionsDialog }
    ?.let { InstructionsDialog(data = it, onDismissed = onInstructionsDismiss) }
}
