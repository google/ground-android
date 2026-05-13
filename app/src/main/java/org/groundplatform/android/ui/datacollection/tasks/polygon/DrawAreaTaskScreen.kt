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
import androidx.core.os.bundleOf
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.DataCollectionFragment
import org.groundplatform.android.ui.datacollection.LoiNameAction
import org.groundplatform.android.ui.datacollection.TaskPosition
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.ui.theme.AppTheme

@Composable
fun DrawAreaTaskScreen(
  viewModel: DrawAreaTaskViewModel,
  taskPosition: TaskPosition? = null,
  shouldShowLoiNameDialog: Boolean,
  loiName: String,
  onButtonClicked: (ButtonAction) -> Unit,
  onLoiNameAction: (LoiNameAction) -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val showSelfIntersectionDialog by viewModel.showSelfIntersectionDialog
  val showInstructionsDialog by viewModel.showInstructionsDialog.collectAsStateWithLifecycle()
  val polygonArea by viewModel.polygonArea.observeAsState()
  val context = LocalContext.current
  val areaMessage = polygonArea?.let { stringResource(R.string.area_message, it) }

  val lifecycleOwner = LocalLifecycleOwner.current
  LaunchedEffect(lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.maybeShowInstructions() }
  }

  LaunchedEffect(areaMessage) {
    areaMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
  }

  DrawAreaTaskContent(
    taskId = viewModel.task.id,
    taskLabel = viewModel.task.label,
    taskPosition = taskPosition,
    taskActionButtonsStates = taskActionButtonsStates,
    showInstructionsDialog = showInstructionsDialog,
    showSelfIntersectionDialog = showSelfIntersectionDialog,
    shouldShowLoiNameDialog = shouldShowLoiNameDialog,
    loiName = loiName,
    onButtonClicked = onButtonClicked,
    onLoiNameAction = onLoiNameAction,
    onInstructionsDismiss = { viewModel.dismissDrawAreaInstructions() },
    onDismissSelfIntersectionDialog = { viewModel.showSelfIntersectionDialog.value = false },
  )
}

/**
 * The stateless content of the draw area task screen.
 *
 * @param taskLabel The label of the task.
 * @param taskPosition The position of the task in the sequence.
 * @param taskActionButtonsStates The states of the action buttons.
 * @param showInstructionsDialog Whether to show the instructions' dialog.
 * @param showSelfIntersectionDialog Whether to show the self-intersection warning dialog.
 * @param shouldShowLoiNameDialog Whether to show the dialog for setting the LOI name.
 * @param loiName Value to be prepopulated in the LOI name dialog.
 * @param onButtonClicked Callback when a button with a [ButtonAction] is clicked.
 * @param onLoiNameAction Callback when user interacts with the LOI name dialog.
 * @param onInstructionsDismiss Callback when user dismisses the instructions dialog.
 * @param onDismissSelfIntersectionDialog Callback when the self-intersection dialog is dismissed.
 */
@Composable
private fun DrawAreaTaskContent(
  taskId: String,
  taskLabel: String,
  taskPosition: TaskPosition? = null,
  taskActionButtonsStates: List<ButtonActionState>,
  showInstructionsDialog: Boolean,
  showSelfIntersectionDialog: Boolean,
  shouldShowLoiNameDialog: Boolean,
  loiName: String,
  onButtonClicked: (ButtonAction) -> Unit,
  onLoiNameAction: (LoiNameAction) -> Unit,
  onInstructionsDismiss: () -> Unit,
  onDismissSelfIntersectionDialog: () -> Unit,
) {
  TaskScreen(
    taskHeader = TaskHeader(taskLabel, R.drawable.outline_draw),
    taskPosition = taskPosition,
    instructionData =
      InstructionData(
        iconId = R.drawable.touch_app_24,
        stringId = R.string.draw_area_task_instruction,
      ),
    taskActionButtonsStates = taskActionButtonsStates,
    showInstructionsDialog = showInstructionsDialog,
    shouldShowLoiNameDialog = shouldShowLoiNameDialog,
    loiName = loiName,
    onButtonClicked = onButtonClicked,
    onLoiNameAction = onLoiNameAction,
    onInstructionsDismiss = onInstructionsDismiss,
    taskBody = {
      AndroidFragment(
        clazz = DrawAreaTaskMapFragment::class.java,
        arguments = bundleOf(Pair(DataCollectionFragment.TASK_ID, taskId)),
      )
    },
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
      taskId = "task_id",
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
      onButtonClicked = {},
      onLoiNameAction = {},
      onInstructionsDismiss = {},
      onDismissSelfIntersectionDialog = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun DrawAreaTaskScreenSelfIntersectionPreview() {
  AppTheme {
    DrawAreaTaskContent(
      taskId = "task_id",
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
      onButtonClicked = {},
      onLoiNameAction = {},
      onInstructionsDismiss = {},
      onDismissSelfIntersectionDialog = {},
    )
  }
}
