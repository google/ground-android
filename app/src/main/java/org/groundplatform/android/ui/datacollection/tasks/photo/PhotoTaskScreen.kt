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
package org.groundplatform.android.ui.datacollection.tasks.photo

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.components.UriImage
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.ui.theme.sizes

@Composable
fun PhotoTaskScreen(
  viewModel: PhotoTaskViewModel,
  onTakePhoto: () -> Unit,
  onFooterPositionUpdated: (Float) -> Unit,
  onAction: (TaskScreenAction) -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val uri by viewModel.uri.collectAsStateWithLifecycle(Uri.EMPTY)
  val showPermissionDeniedDialog by
    viewModel.showPermissionDeniedDialog.collectAsStateWithLifecycle()

  TaskScreen(
    taskHeader =
      TaskHeader(label = viewModel.task.label, iconResId = R.drawable.ic_question_answer),
    taskActionButtonsStates = taskActionButtonsStates,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onAction = onAction,
    taskBody = {
      PhotoTaskContent(
        uri = uri,
        onTakePhoto = onTakePhoto,
        modifier = Modifier.padding(horizontal = MaterialTheme.sizes.taskViewPadding),
      )
    },
  )

  if (showPermissionDeniedDialog) {
    ConfirmationDialog(
      title = R.string.permission_denied,
      description = R.string.camera_permissions_needed,
      confirmButtonText = R.string.ok,
      onConfirmClicked = { viewModel.setShowPermissionDeniedDialog(false) },
    )
  }
}

@Composable
internal fun PhotoTaskContent(uri: Uri, onTakePhoto: () -> Unit, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
    if (uri == Uri.EMPTY) {
      CaptureButton(onTakePhoto)
    } else {
      UriImage(uri = uri, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
  }
}

@Composable
private fun CaptureButton(onTakePhoto: () -> Unit) {
  FilledTonalButton(
    onClick = onTakePhoto,
    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
  ) {
    Icon(
      imageVector = ImageVector.vectorResource(id = R.drawable.outline_photo_camera),
      contentDescription = stringResource(id = R.string.camera),
      modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    Text(text = stringResource(id = R.string.camera))
  }
}
