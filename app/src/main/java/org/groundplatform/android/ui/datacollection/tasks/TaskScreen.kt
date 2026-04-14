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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.InstructionsDialog
import org.groundplatform.android.ui.datacollection.components.LoiNameDialog
import org.groundplatform.android.ui.datacollection.components.TaskFooter
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.components.TaskViewLayout

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskScreen(
  taskHeader: TaskHeader?,
  instructionData: InstructionData? = null,
  taskActionButtonsStates: List<ButtonActionState>,
  shouldShowLoiNameDialog: Boolean = false,
  showInstructionsDialog: Boolean = false,
  loiName: String = "",
  onFooterPositionUpdated: (Float) -> Unit,
  onAction: (TaskScreenAction) -> Unit,
  footerContent: @Composable (() -> Unit)? = null,
  taskBody: @Composable () -> Unit,
) {
  val isKeyboardOpen = WindowInsets.isImeVisible
  var footerPositionY by remember { mutableFloatStateOf(0f) }

  // Update footer position whenever layout changes or keyboard is toggled.
  LaunchedEffect(isKeyboardOpen, footerPositionY) { onFooterPositionUpdated(footerPositionY) }

  TaskViewLayout(
    header = taskHeader,
    footer = {
      TaskFooter(
        modifier = Modifier.onGloballyPositioned { footerPositionY = it.positionInWindow().y },
        content = footerContent,
        buttonActionStates = taskActionButtonsStates,
        onButtonClicked = { onAction(TaskScreenAction.OnButtonClicked(it)) },
      )
    },
    content = { taskBody() },
  )

  if (shouldShowLoiNameDialog) {
    LoiNameDialog(
      textFieldValue = loiName,
      onConfirmRequest = { onAction(TaskScreenAction.OnLoiNameConfirm(loiName)) },
      onDismissRequest = { onAction(TaskScreenAction.OnLoiNameDismiss) },
      onTextFieldChange = { onAction(TaskScreenAction.OnLoiNameChanged(it)) },
    )
  }

  instructionData
    ?.takeIf { showInstructionsDialog }
    ?.let {
      InstructionsDialog(
        data = it,
        onDismissed = { onAction(TaskScreenAction.OnInstructionsDismiss) },
      )
    }
}
