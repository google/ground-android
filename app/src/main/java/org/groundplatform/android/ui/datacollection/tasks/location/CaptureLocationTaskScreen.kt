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
package org.groundplatform.android.ui.datacollection.tasks.location

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.DataCollectionFragment
import org.groundplatform.android.ui.datacollection.TaskPosition
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.ui.theme.AppTheme

/**
 * A screen for capturing the user's current location.
 *
 * This is the stateful wrapper that collects state from [CaptureLocationTaskViewModel] and handles
 * event routing.
 */
@Composable
fun CaptureLocationTaskScreen(
  viewModel: CaptureLocationTaskViewModel,
  taskPosition: TaskPosition? = null,
  onButtonClicked: (ButtonAction) -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val showAccuracyCard by viewModel.showAccuracyCard.collectAsStateWithLifecycle()
  val showPermissionDeniedDialog by
    viewModel.showPermissionDeniedDialog.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) { viewModel.enableLocationLock() }

  CaptureLocationTaskContent(
    taskId = viewModel.task.id,
    taskLabel = viewModel.task.label,
    taskPosition = taskPosition,
    taskActionButtonsStates = taskActionButtonsStates,
    showAccuracyCard = showAccuracyCard,
    showPermissionDeniedDialog = showPermissionDeniedDialog,
    onDismissAccuracyCard = { viewModel.dismissAccuracyCard() },
    onAllowLocationClicked = { viewModel.onAllowLocationClicked() },
    onButtonClicked = onButtonClicked,
  )
}

/**
 * The stateless content of the capture location task screen.
 *
 * @param taskLabel The label of the task.
 * @param taskPosition The position of the task in the sequence.
 * @param taskActionButtonsStates The states of the action buttons.
 * @param showAccuracyCard Whether to show the accuracy warning card.
 * @param showPermissionDeniedDialog Whether to show the permission denied dialog.
 * @param onDismissAccuracyCard Callback when the accuracy card is dismissed.
 * @param onAllowLocationClicked Callback when the allow location button is clicked in the dialog.
 * @param onButtonClicked Callback when a button is clicked.
 */
@Composable
private fun CaptureLocationTaskContent(
  taskId: String,
  taskLabel: String,
  taskPosition: TaskPosition? = null,
  taskActionButtonsStates: List<ButtonActionState>,
  showAccuracyCard: Boolean,
  showPermissionDeniedDialog: Boolean,
  onDismissAccuracyCard: () -> Unit,
  onAllowLocationClicked: () -> Unit,
  onButtonClicked: (ButtonAction) -> Unit,
) {
  TaskScreen(
    taskHeader = TaskHeader(taskLabel, R.drawable.outline_pin_drop),
    taskPosition = taskPosition,
    taskActionButtonsStates = taskActionButtonsStates,
    onButtonClicked = onButtonClicked,
    footerContent = {
      if (showAccuracyCard) {
        LocationAccuracyCard(
          onDismiss = onDismissAccuracyCard,
          modifier = Modifier.padding(bottom = 12.dp),
        )
      }
    },
    taskBody = {
      AndroidFragment(
        clazz = CaptureLocationTaskMapFragment::class.java,
        arguments = bundleOf(Pair(DataCollectionFragment.TASK_ID, taskId)),
      )

      if (showPermissionDeniedDialog) {
        ConfirmationDialog(
          title = R.string.allow_location_title,
          description = R.string.allow_location_description,
          confirmButtonText = R.string.allow_location_confirmation,
          onConfirmClicked = onAllowLocationClicked,
        )
      }
    },
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun CaptureLocationTaskScreenPreview() {
  AppTheme {
    CaptureLocationTaskContent(
      taskId = "task_id",
      taskLabel = "Task for capturing current location",
      taskActionButtonsStates =
        listOf(
          ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.UNDO, isEnabled = false, isVisible = false),
          ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = true, isVisible = true),
          ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
        ),
      showAccuracyCard = true,
      showPermissionDeniedDialog = true,
      onDismissAccuracyCard = {},
      onAllowLocationClicked = {},
      onButtonClicked = {},
    )
  }
}
