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
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.InstructionsDialog
import org.groundplatform.android.ui.datacollection.components.LoiNameDialog
import org.groundplatform.android.ui.datacollection.components.TaskFooter
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.components.TaskViewLayout

@OptIn(ExperimentalLayoutApi::class)
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
  val isKeyboardOpen = WindowInsets.isImeVisible
  var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()

  // Update footer position whenever layout changes or keyboard is toggled.
  LaunchedEffect(isKeyboardOpen, layoutCoordinates) {
    layoutCoordinates?.let { dataCollectionViewModel.updateFooterPosition(it.positionInWindow().y) }
  }

  val handleNext = {
    if (viewModel.task.isAddLoiTask) {
      dataCollectionViewModel.loiNameDialogOpen.value = true
    } else {
      dataCollectionViewModel.onNextClicked(viewModel)
    }
  }

  val handleButtonClick = { action: ButtonAction ->
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

  TaskViewLayout(
    header = taskHeader,
    footer = {
      TaskFooter(
        modifier = Modifier.onGloballyPositioned { layoutCoordinates = it },
        headerCard = if (shouldShowHeader && headerCard != null) headerCard else null,
        buttonActionStates = taskActionButtonsStates,
        onButtonClicked = handleButtonClick,
      )
    },
    content = content,
  )

  if (viewModel.task.isAddLoiTask) {
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
          if (name != "") {
            dataCollectionViewModel.setLoiName(name)
            dataCollectionViewModel.onNextClicked(viewModel)
          }
        },
        onDismissRequest = {
          name = initialNameValue
          openAlertDialog = false
        },
        onTextFieldChange = { name = it },
      )
    }
  }

  instructionData?.let {
    var showInstructionsDialog by viewModel.showInstructionsDialog
    if (showInstructionsDialog) {
      InstructionsDialog(
        data = it,
        onDismissed = {
          showInstructionsDialog = false
          onInstructionDialogDismissed()
        },
      )
    }
  }
}
