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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction

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

  TaskScreen(
    taskHeader = TaskHeader(viewModel.task.label, R.drawable.outline_pin_drop),
    taskActionButtonsStates = taskActionButtonsStates,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onAction = onAction,
    shouldShowHeader = true,
    headerCard = {
      if (showAccuracyCard) {
        LocationAccuracyCard(
          onDismiss = { viewModel.dismissAccuracyCard() },
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
            viewModel.onAllowLocationClicked()
            onOpenSettings()
          },
        )
      }
    },
  )
}
