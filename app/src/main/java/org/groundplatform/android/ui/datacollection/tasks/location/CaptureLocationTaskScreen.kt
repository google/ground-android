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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.ui.theme.AppTheme

/**
 * A screen for capturing the user's current location.
 *
 * This is the stateful wrapper that collects state from [CaptureLocationTaskViewModel] and
 * handles event routing.
 *
 * @param viewModel The view model for this task.
 * @param onFooterPositionUpdated Callback when the footer position changes.
 * @param onAction Callback for screen actions (e.g., navigation).
 * @param onOpenSettings Callback to open app settings when permission is denied.
 * @param mapContent Composable for rendering the map.
 */
@Composable
fun CaptureLocationTaskScreen(
  viewModel: CaptureLocationTaskViewModel,
  onFooterPositionUpdated: (Float) -> Unit,
  onAction: (TaskScreenAction) -> Unit,
  onOpenSettings: () -> Unit,
  mapContent: @Composable () -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val showAccuracyCard by viewModel.showAccuracyCard.collectAsStateWithLifecycle()
  val showPermissionDeniedDialog by
    viewModel.showPermissionDeniedDialog.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) { viewModel.enableLocationLock() }

  CaptureLocationTaskContent(
    taskLabel = viewModel.task.label,
    taskActionButtonsStates = taskActionButtonsStates,
    showAccuracyCard = showAccuracyCard,
    showPermissionDeniedDialog = showPermissionDeniedDialog,
    onDismissAccuracyCard = { viewModel.dismissAccuracyCard() },
    onAllowLocationClicked = { viewModel.onAllowLocationClicked() },
    onOpenSettings = onOpenSettings,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onAction = onAction,
    mapContent = mapContent,
  )
}

/**
 * The stateless content of the capture location task screen.
 *
 * @param taskLabel The label of the task.
 * @param taskActionButtonsStates The states of the action buttons.
 * @param showAccuracyCard Whether to show the accuracy warning card.
 * @param showPermissionDeniedDialog Whether to show the permission denied dialog.
 * @param onDismissAccuracyCard Callback when the accuracy card is dismissed.
 * @param onAllowLocationClicked Callback when the allow location button is clicked in the dialog.
 * @param onOpenSettings Callback to open app settings.
 * @param onFooterPositionUpdated Callback when the footer position changes.
 * @param onAction Callback for screen actions.
 * @param mapContent Composable for rendering the map.
 */
@Composable
private fun CaptureLocationTaskContent(
  taskLabel: String,
  taskActionButtonsStates: List<ButtonActionState>,
  showAccuracyCard: Boolean,
  showPermissionDeniedDialog: Boolean,
  onDismissAccuracyCard: () -> Unit,
  onAllowLocationClicked: () -> Unit,
  onOpenSettings: () -> Unit,
  onFooterPositionUpdated: (Float) -> Unit,
  onAction: (TaskScreenAction) -> Unit,
  mapContent: @Composable () -> Unit,
) {
  TaskScreen(
    taskHeader = TaskHeader(taskLabel, R.drawable.outline_pin_drop),
    taskActionButtonsStates = taskActionButtonsStates,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onAction = onAction,
    shouldShowHeader = true,
    headerCard = {
      if (showAccuracyCard) {
        LocationAccuracyCard(
          onDismiss = onDismissAccuracyCard,
          modifier = Modifier.padding(bottom = 12.dp),
        )
      }
    },
    taskBody = {
      mapContent()

      if (showPermissionDeniedDialog) {
        ConfirmationDialog(
          title = R.string.allow_location_title,
          description = R.string.allow_location_description,
          confirmButtonText = R.string.allow_location_confirmation,
          onConfirmClicked = {
            onAllowLocationClicked()
            onOpenSettings()
          },
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
      onOpenSettings = {},
      onFooterPositionUpdated = {},
      onAction = {},
      mapContent = {},
    )
  }
}
