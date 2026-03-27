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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.InstructionsDialog
import org.groundplatform.android.ui.datacollection.components.LoiNameDialog
import org.groundplatform.android.ui.datacollection.components.TaskFooter
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.components.TaskViewLayout
import org.groundplatform.domain.model.task.Task

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskScreen(
  task: Task,
  taskHeader: TaskHeader?,
  instructionData: InstructionData?,
  taskActionButtonsStates: List<ButtonActionState>,
  loiNameDialogOpen: Boolean,
  shouldShowHeader: Boolean,
  showInstructionsDialog: Boolean,
  initialNameValue: String,
  onFooterPositionUpdated: (Float) -> Unit,
  onButtonClicked: (ButtonAction) -> Unit,
  onLoiNameConfirm: (String) -> Unit,
  onLoiNameDismiss: () -> Unit,
  onInstructionsDismiss: () -> Unit,
  headerCard: @Composable (() -> Unit)?,
  taskBody: @Composable () -> Unit,
  state: TaskScreenState = rememberTaskScreenState(initialLoiName = initialNameValue)
) {
  val isKeyboardOpen = WindowInsets.isImeVisible

  // Update footer position whenever layout changes or keyboard is toggled.
  LaunchedEffect(isKeyboardOpen, state.layoutCoordinates) {
    state.layoutCoordinates?.let { onFooterPositionUpdated(it.positionInWindow().y) }
  }

  TaskViewLayout(
    header = taskHeader,
    footer = {
      TaskFooter(
        modifier = Modifier.onGloballyPositioned { state.layoutCoordinates = it },
        headerCard = headerCard.takeIf { shouldShowHeader },
        buttonActionStates = taskActionButtonsStates,
        onButtonClicked = onButtonClicked,
      )
    },
    content = { taskBody() },
  )

  if (task.isAddLoiTask && loiNameDialogOpen) {
    LoiNameDialog(
      textFieldValue = state.loiName,
      onConfirmRequest = { onLoiNameConfirm(state.loiName) },
      onDismissRequest = {
        state.setLoiName(initialNameValue)
        onLoiNameDismiss()
      },
      onTextFieldChange = { state.loiName = it },
    )
  }

  instructionData
    ?.takeIf { showInstructionsDialog }
    ?.let { InstructionsDialog(data = it, onDismissed = onInstructionsDismiss) }
}
