/*
 * Copyright 2022 Google LLC
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

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.system.PermissionDeniedException
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.tasks.TaskContainer
import org.groundplatform.android.ui.home.HomeScreenViewModel
import org.groundplatform.ui.theme.sizes
import timber.log.Timber

@Composable
fun PhotoTaskScreen(
  viewModel: PhotoTaskViewModel,
  dataCollectionViewModel: DataCollectionViewModel,
  homeScreenViewModel: HomeScreenViewModel,
  permissionsManager: PermissionsManager,
  popups: EphemeralPopups,
) {
  var showPermissionDeniedDialog by viewModel.showPermissionDeniedDialog
  val uri by viewModel.uri.collectAsStateWithLifecycle(Uri.EMPTY)
  val scope = rememberCoroutineScope()
  var hasRequestedPermissionsOnResume by remember { mutableStateOf(false) }

  val capturePhotoLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { result: Boolean ->
      viewModel.onCaptureResult(result)
    }

  val launchPhotoCapture = {
    scope.launch {
      try {
        viewModel.waitForPhotoCapture(viewModel.task.id)
        val imageUri = viewModel.createImageFileUri()
        viewModel.capturedUri = imageUri
        viewModel.hasLaunchedCamera = true
        capturePhotoLauncher.launch(imageUri)
        Timber.d("Capture photo intent sent")
      } catch (e: IllegalArgumentException) {
        homeScreenViewModel.awaitingPhotoCapture = false
        popups.ErrorPopup().unknownError()
        Timber.e(e)
      }
    }
  }

  val obtainCapturePhotoPermissions = { onPermissionsGranted: () -> Unit ->
    scope.launch {
      try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
          permissionsManager.obtainPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        permissionsManager.obtainPermission(Manifest.permission.CAMERA)
        onPermissionsGranted()
      } catch (_: PermissionDeniedException) {
        viewModel.showPermissionDeniedDialog.value = true
      }
    }
  }

  val onTakePhoto = {
    if (!viewModel.hasLaunchedCamera) {
      homeScreenViewModel.awaitingPhotoCapture = true
      obtainCapturePhotoPermissions { launchPhotoCapture() }
    }
  }

  LaunchedEffect(Unit) {
    viewModel.surveyId = dataCollectionViewModel.requireSurveyId()
    if (!hasRequestedPermissionsOnResume) {
      obtainCapturePhotoPermissions {}
      hasRequestedPermissionsOnResume = true
    }
  }

  TaskContainer(viewModel = viewModel, dataCollectionViewModel = dataCollectionViewModel) {
    PhotoTaskScreen(
      modifier = Modifier.padding(horizontal = MaterialTheme.sizes.taskViewPadding),
      uri = uri,
      onTakePhoto = onTakePhoto,
    )

    if (showPermissionDeniedDialog) {
      ConfirmationDialog(
        title = R.string.permission_denied,
        description = R.string.camera_permissions_needed,
        confirmButtonText = R.string.ok,
        onConfirmClicked = { showPermissionDeniedDialog = false },
      )
    }
  }
}
