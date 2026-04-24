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
package org.groundplatform.android.ui.datacollection.tasks.polygon

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.ui.theme.AppTheme

@Composable
fun DrawAreaTaskScreen(
  viewModel: DrawAreaTaskViewModel,
  onFooterPositionUpdated: (Float) -> Unit,
  shouldShowLoiNameDialog: Boolean,
  loiName: String,
  onAction: (TaskScreenAction) -> Unit,
  mapContent: @Composable () -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val showSelfIntersectionDialog by viewModel.showSelfIntersectionDialog
  val showInstructionsDialog by viewModel.showInstructionsDialog.collectAsStateWithLifecycle()
  val polygonArea by viewModel.polygonArea.observeAsState()
  val context = LocalContext.current
  val areaMessage = polygonArea?.let { stringResource(R.string.area_message, it) }

  val lifecycleOwner = LocalLifecycleOwner.current
  LaunchedEffect(lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      viewModel.maybeShowInstructions()
    }
  }

  LaunchedEffect(areaMessage) {
    areaMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
  }

  DrawAreaTaskContent(
    taskLabel = viewModel.task.label,
    taskActionButtonsStates = taskActionButtonsStates,
    showInstructionsDialog = showInstructionsDialog,
    showSelfIntersectionDialog = showSelfIntersectionDialog,
    shouldShowLoiNameDialog = shouldShowLoiNameDialog,
    loiName = loiName,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onAction = { action ->
      if (action is TaskScreenAction.OnInstructionsDismiss) {
        viewModel.dismissDrawAreaInstructions()
      } else {
        onAction(action)
      }
    },
    onDismissSelfIntersectionDialog = { viewModel.showSelfIntersectionDialog.value = false },
    mapContent = mapContent,
  )
}

@Composable
private fun DrawAreaTaskContent(
  taskLabel: String,
  taskActionButtonsStates: List<ButtonActionState>,
  showInstructionsDialog: Boolean,
  showSelfIntersectionDialog: Boolean,
  shouldShowLoiNameDialog: Boolean,
  loiName: String,
  onFooterPositionUpdated: (Float) -> Unit,
  onAction: (TaskScreenAction) -> Unit,
  onDismissSelfIntersectionDialog: () -> Unit,
  mapContent: @Composable () -> Unit,
) {
  TaskScreen(
    taskHeader = TaskHeader(taskLabel, R.drawable.outline_draw),
    instructionData =
      InstructionData(
        iconId = R.drawable.touch_app_24,
        stringId = R.string.draw_area_task_instruction,
      ),
    taskActionButtonsStates = taskActionButtonsStates,
    showInstructionsDialog = showInstructionsDialog,
    shouldShowLoiNameDialog = shouldShowLoiNameDialog,
    loiName = loiName,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onAction = onAction,
    taskBody = { mapContent() },
  )

  if (showSelfIntersectionDialog) {
    ConfirmationDialog(
      title = R.string.polygon_vertex_add_dialog_title,
      description = R.string.polygon_vertex_add_dialog_message,
      confirmButtonText = R.string.polygon_vertex_add_dialog_positive_button,
      dismissButtonText = null,
      onConfirmClicked = onDismissSelfIntersectionDialog,
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun DrawAreaTaskScreenInstructionsPreview() {
  AppTheme {
    DrawAreaTaskContent(
      taskLabel = "Task for drawing a polygon",
      taskActionButtonsStates =
        listOf(
          ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.UNDO, isEnabled = false, isVisible = true),
          ButtonActionState(ButtonAction.REDO, isEnabled = false, isVisible = true),
          ButtonActionState(ButtonAction.ADD_POINT, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.COMPLETE, isEnabled = false, isVisible = false),
        ),
      showInstructionsDialog = true,
      showSelfIntersectionDialog = false,
      shouldShowLoiNameDialog = false,
      loiName = "",
      onFooterPositionUpdated = {},
      onAction = {},
      onDismissSelfIntersectionDialog = {},
      mapContent = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun DrawAreaTaskScreenSelfIntersectionPreview() {
  AppTheme {
    DrawAreaTaskContent(
      taskLabel = "Task for drawing a polygon",
      taskActionButtonsStates =
        listOf(
          ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.UNDO, isEnabled = false, isVisible = true),
          ButtonActionState(ButtonAction.REDO, isEnabled = false, isVisible = true),
          ButtonActionState(ButtonAction.ADD_POINT, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.COMPLETE, isEnabled = false, isVisible = false),
        ),
      showInstructionsDialog = false,
      showSelfIntersectionDialog = true,
      shouldShowLoiNameDialog = false,
      loiName = "",
      onFooterPositionUpdated = {},
      onAction = {},
      onDismissSelfIntersectionDialog = {},
      mapContent = {},
    )
  }
}
