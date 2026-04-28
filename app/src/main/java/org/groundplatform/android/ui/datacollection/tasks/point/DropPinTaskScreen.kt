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
package org.groundplatform.android.ui.datacollection.tasks.point

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.LoiNameAction
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.ui.theme.AppTheme

/**
 * A screen for dropping a pin on the map.
 *
 * This is the stateful wrapper that collects state from [DropPinTaskViewModel] and handles event
 * routing.
 *
 * @param viewModel The view model for this task.
 * @param onFooterPositionUpdated Callback when the footer position changes.
 * @param onAction Callback for screen actions (e.g., navigation).
 * @param mapContent Composable for rendering the map.
 */
@Composable
fun DropPinTaskScreen(
  viewModel: DropPinTaskViewModel,
  onFooterPositionUpdated: (Float) -> Unit,
  shouldShowLoiNameDialog: Boolean,
  loiName: String,
  onButtonClicked: (ButtonAction) -> Unit,
  onLoiNameAction: (LoiNameAction) -> Unit,
  mapContent: @Composable () -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val showInstructionsDialog by viewModel.showInstructionsDialog.collectAsStateWithLifecycle()

  val lifecycleOwner = LocalLifecycleOwner.current
  LaunchedEffect(lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.maybeShowInstructions() }
  }

  DropPinTaskContent(
    taskLabel = viewModel.task.label,
    taskActionButtonsStates = taskActionButtonsStates,
    showInstructionsDialog = showInstructionsDialog,
    shouldShowLoiNameDialog = shouldShowLoiNameDialog,
    loiName = loiName,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onButtonClicked = onButtonClicked,
    onLoiNameAction = onLoiNameAction,
    onInstructionsDismiss = { viewModel.dismissDropPinInstructions() },
    mapContent = mapContent,
  )
}

/**
 * The stateless content of the drop pin task screen.
 *
 * @param taskLabel The label of the task.
 * @param taskActionButtonsStates The states of the action buttons.
 * @param showInstructionsDialog Whether to show the instructions' dialog.
 * @param onFooterPositionUpdated Callback when the footer position changes.
 * @param mapContent Composable for rendering the map.
 */
@Composable
private fun DropPinTaskContent(
  taskLabel: String,
  taskActionButtonsStates: List<ButtonActionState>,
  showInstructionsDialog: Boolean,
  shouldShowLoiNameDialog: Boolean,
  loiName: String,
  onFooterPositionUpdated: (Float) -> Unit,
  onButtonClicked: (ButtonAction) -> Unit,
  onLoiNameAction: (LoiNameAction) -> Unit,
  onInstructionsDismiss: () -> Unit,
  mapContent: @Composable () -> Unit,
) {
  TaskScreen(
    taskHeader = TaskHeader(taskLabel, R.drawable.outline_pin_drop),
    instructionData =
      InstructionData(iconId = R.drawable.swipe_24, stringId = R.string.drop_a_pin_tooltip_text),
    taskActionButtonsStates = taskActionButtonsStates,
    showInstructionsDialog = showInstructionsDialog,
    shouldShowLoiNameDialog = shouldShowLoiNameDialog,
    loiName = loiName,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onButtonClicked = onButtonClicked,
    onLoiNameAction = onLoiNameAction,
    onInstructionsDismiss = onInstructionsDismiss,
    taskBody = mapContent,
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun DropPinTaskScreenPreview() {
  AppTheme {
    DropPinTaskContent(
      taskLabel = "Task for dropping a pin",
      taskActionButtonsStates =
        listOf(
          ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.UNDO, isEnabled = false, isVisible = false),
          ButtonActionState(ButtonAction.DROP_PIN, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
        ),
      showInstructionsDialog = true,
      shouldShowLoiNameDialog = false,
      loiName = "",
      onFooterPositionUpdated = {},
      onButtonClicked = {},
      onLoiNameAction = {},
      onInstructionsDismiss = {},
      mapContent = {},
    )
  }
}
