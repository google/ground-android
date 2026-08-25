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
import androidx.compose.runtime.Composable
import org.groundplatform.android.ui.datacollection.LoiNameAction
import org.groundplatform.android.ui.datacollection.TaskPosition
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.InstructionsDialog
import org.groundplatform.android.ui.datacollection.components.LoiNameDialog
import org.groundplatform.android.ui.datacollection.components.TaskFooter
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.components.TaskViewLayout

/** A shared Composable that provides the standard layout for a task. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskScreen(
  taskHeader: TaskHeader?,
  taskPosition: TaskPosition? = null,
  instructionData: InstructionData? = null,
  taskActionButtonsStates: List<ButtonActionState>,
  shouldShowLoiNameDialog: Boolean = false,
  showInstructionsDialog: Boolean = false,
  loiName: String = "",
  onButtonClicked: (ButtonAction) -> Unit = {},
  onLoiNameAction: (LoiNameAction) -> Unit = {},
  onInstructionsDismiss: () -> Unit = {},
  footerContent: @Composable (() -> Unit)? = null,
  taskBody: @Composable () -> Unit,
) {
  TaskViewLayout(
    header = taskHeader,
    footer = {
      TaskFooter(
        taskPosition = taskPosition,
        content = footerContent,
        buttonActionStates = taskActionButtonsStates,
        onButtonClicked = onButtonClicked,
      )
    },
    content = { taskBody() },
  )

  if (shouldShowLoiNameDialog) {
    LoiNameDialog(textFieldValue = loiName, onLoiNameAction = onLoiNameAction)
  }

  instructionData
    ?.takeIf { showInstructionsDialog }
    ?.let { InstructionsDialog(data = it, onDismissed = onInstructionsDismiss) }
}
