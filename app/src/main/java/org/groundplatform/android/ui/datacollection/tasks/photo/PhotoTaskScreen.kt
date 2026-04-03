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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
  onFooterPositionUpdated: (Float) -> Unit,
  onAction: (TaskScreenAction) -> Unit,
  onAwaitingPhotoCapture: (Boolean) -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val uri by viewModel.uri.collectAsStateWithLifecycle(Uri.EMPTY)
  val event by viewModel.events.collectAsStateWithLifecycle(initialValue = null)
  val isAwaiting by viewModel.isAwaitingPhotoCapture.collectAsStateWithLifecycle()

  var activeError by remember { mutableStateOf<PhotoTaskEvent.ShowError?>(null) }

  LaunchedEffect(isAwaiting) { onAwaitingPhotoCapture(isAwaiting) }

  val capturePhotoLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { result ->
      viewModel.onCaptureResult(result)
    }

  LaunchedEffect(event) {
    when (val currentEvent = event) {
      is PhotoTaskEvent.LaunchCamera -> capturePhotoLauncher.launch(currentEvent.uri)
      is PhotoTaskEvent.ShowError -> {
        activeError = currentEvent
      }
      null -> {
        /* Do nothing */
      }
    }
  }

  TaskScreen(
    taskHeader =
      TaskHeader(label = viewModel.task.label, iconResId = R.drawable.ic_question_answer),
    taskActionButtonsStates = taskActionButtonsStates,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onAction = onAction,
    taskBody = {
      PhotoTaskContent(
        uri = uri,
        onTakePhoto = { viewModel.onTakePhoto() },
        activeError = activeError,
        onDismissError = { activeError = null },
        modifier = Modifier.padding(horizontal = MaterialTheme.sizes.taskViewPadding),
      )
    },
  )
}

@Composable
internal fun PhotoTaskContent(
  uri: Uri,
  onTakePhoto: () -> Unit,
  activeError: PhotoTaskEvent.ShowError?,
  onDismissError: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
    if (uri == Uri.EMPTY) {
      CaptureButton(onTakePhoto)
    } else {
      UriImage(uri = uri, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
  }

  activeError?.let { errorEvent ->
    val (titleResId, messageResId) =
      when (errorEvent.errorType) {
        PhotoTaskError.PERMISSION_DENIED ->
          Pair(R.string.permission_denied, R.string.camera_permissions_needed)
        PhotoTaskError.CAMERA_LAUNCH_FAILED ->
          Pair(R.string.camera_launch_failed_title, R.string.camera_launch_failed_desc)
        PhotoTaskError.PHOTO_SAVE_FAILED ->
          Pair(R.string.photo_save_failed_title, R.string.photo_save_failed_desc)
      }
    ConfirmationDialog(
      title = titleResId,
      description = messageResId,
      confirmButtonText = R.string.ok,
      onConfirmClicked = onDismissError,
    )
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

@Preview(showBackground = true)
@Composable
private fun PhotoTaskContent_Empty() {
  PhotoTaskContent(uri = Uri.EMPTY, onTakePhoto = {}, activeError = null, onDismissError = {})
}

@Preview(showBackground = true)
@Composable
private fun PhotoTaskContent_WithPhoto() {
  PhotoTaskContent(
    uri = "content://mock/uri".toUri(),
    onTakePhoto = {},
    activeError = null,
    onDismissError = {},
  )
}

@Preview(showBackground = true)
@Composable
private fun PhotoTaskContent_WithError() {
  PhotoTaskContent(
    uri = Uri.EMPTY,
    onTakePhoto = {},
    activeError = PhotoTaskEvent.ShowError(PhotoTaskError.CAMERA_LAUNCH_FAILED),
    onDismissError = {},
  )
}
