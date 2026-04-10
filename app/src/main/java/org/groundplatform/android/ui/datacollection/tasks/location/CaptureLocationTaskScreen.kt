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

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.tasks.LocationLockEnabledState
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction

@Composable
fun CaptureLocationTaskScreen(
  viewModel: CaptureLocationTaskViewModel,
  onFooterPositionUpdated: (Float) -> Unit,
  onAction: (TaskScreenAction) -> Unit,
  mapContent: @Composable () -> Unit,
) {
  val taskActionButtonStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val location by viewModel.lastLocation.collectAsStateWithLifecycle()
  var showPermissionDeniedDialog by viewModel.showPermissionDeniedDialog
  var showAccuracyCard by remember { mutableStateOf(false) }

  LaunchedEffect(location) {
    showAccuracyCard = location != null && !viewModel.isCaptureEnabled.first()
  }

  LaunchedEffect(Unit) {
    viewModel.enableLocationLock()
    viewModel.enableLocationLockFlow.collect {
      if (it == LocationLockEnabledState.NEEDS_ENABLE) {
        showPermissionDeniedDialog = true
      }
    }
  }

  TaskScreen(
    taskHeader = TaskHeader(viewModel.task.label, R.drawable.outline_pin_drop),
    taskActionButtonsStates = taskActionButtonStates,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onAction = onAction,
    shouldShowHeader = true,
    headerCard = {
      if (showAccuracyCard) {
        LocationAccuracyCard(
          onDismiss = { showAccuracyCard = false },
          modifier = Modifier.padding(bottom = 12.dp),
        )
      }
    },
    taskBody = {
      mapContent()

      if (showPermissionDeniedDialog) {
        val context = LocalContext.current
        ConfirmationDialog(
          title = R.string.allow_location_title,
          description = R.string.allow_location_description,
          confirmButtonText = R.string.allow_location_confirmation,
          onConfirmClicked = {
            showPermissionDeniedDialog = false

            // Open the app settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(intent)
          },
        )
      }
    },
  )
}
