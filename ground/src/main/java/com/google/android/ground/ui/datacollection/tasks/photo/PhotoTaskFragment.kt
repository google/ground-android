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
package com.google.android.ground.ui.datacollection.tasks.photo

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.google.android.ground.BuildConfig
import com.google.android.ground.R
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.coroutines.MainScope
import com.google.android.ground.databinding.PhotoTaskFragBinding
import com.google.android.ground.repository.UserMediaRepository
import com.google.android.ground.system.PermissionDeniedException
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/** Fragment allowing the user to capture a photo to complete a task. */
@AndroidEntryPoint
class PhotoTaskFragment : AbstractTaskFragment<PhotoTaskViewModel>() {
  @Inject lateinit var userMediaRepository: UserMediaRepository
  @Inject @ApplicationScope lateinit var externalScope: CoroutineScope
  @Inject @MainScope lateinit var mainScope: CoroutineScope
  @Inject lateinit var permissionsManager: PermissionsManager
  @Inject lateinit var popups: EphemeralPopups
  @Inject lateinit var navigator: Navigator

  // Registers a callback to execute after a user captures a photo from the on-device camera.
  private var capturePhotoLauncher: ActivityResultLauncher<Uri> =
    registerForActivityResult(ActivityResultContracts.TakePicture()) { result: Boolean ->
      if (result) viewModel.savePhotoTaskData(capturedPhotoUri)
    }

  private var hasRequestedPermissionsOnResume = false
  private var taskWaitingForPhoto: String? = null
  private var capturedPhotoPath: String? = null
  private lateinit var capturedPhotoUri: Uri

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val taskBinding = PhotoTaskFragBinding.inflate(inflater)
    taskBinding.lifecycleOwner = this
    taskBinding.fragment = this
    taskBinding.dataCollectionViewModel = dataCollectionViewModel
    taskBinding.viewModel = viewModel
    return taskBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    taskWaitingForPhoto = savedInstanceState?.getString(TASK_WAITING_FOR_PHOTO)
    capturedPhotoPath = savedInstanceState?.getString(CAPTURED_PHOTO_PATH)
  }

  override fun onTaskViewAttached() {
    viewModel.surveyId = dataCollectionViewModel.surveyId
    viewModel.taskWaitingForPhoto = taskWaitingForPhoto
  }

  override fun onCreateActionButtons() {
    addUndoButton()
    addSkipButton()
    addNextButton()
  }

  override fun onResume() {
    super.onResume()

    if (!hasRequestedPermissionsOnResume) {
      obtainCapturePhotoPermissions()
      hasRequestedPermissionsOnResume = true
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(TASK_WAITING_FOR_PHOTO, viewModel.taskWaitingForPhoto)
    outState.putString(CAPTURED_PHOTO_PATH, capturedPhotoPath)
  }

  // Requests camera/photo access permissions from the device, executing an optional callback
  // when permission is granted.
  private fun obtainCapturePhotoPermissions(onPermissionsGranted: () -> Unit = {}) {
    externalScope.launch {
      try {

        // From Android 11 onwards (api level 30), requesting WRITE_EXTERNAL_STORAGE permission
        // always returns denied. By default, the app has read/write access to shared data.
        //
        // For more details please refer to:
        // https://developer.android.com/about/versions/11/privacy/storage#permissions-target-11
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
          permissionsManager.obtainPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        permissionsManager.obtainPermission(Manifest.permission.CAMERA)

        onPermissionsGranted()
      } catch (_: PermissionDeniedException) {
        mainScope.launch {
          (view as ViewGroup).addView(
            ComposeView(requireContext()).apply {
              setContent { AppTheme { PermissionDeniedDialog() } }
            }
          )
        }
      }
    }
  }

  @Composable
  fun PermissionDeniedDialog() {
    val openDialog = remember { mutableStateOf(true) }

    fun dismissDialog() {
      openDialog.value = false
    }

    if (openDialog.value) {
      AlertDialog(
        onDismissRequest = { dismissDialog() },
        title = { Text(text = stringResource(R.string.permission_denied)) },
        text = { Text(text = stringResource(R.string.camera_permissions_needed)) },
        confirmButton = {
          TextButton(onClick = { dismissDialog() }) { Text(text = stringResource(R.string.ok)) }
        },
      )
    }
  }

  fun onTakePhoto() {
    obtainCapturePhotoPermissions { launchPhotoCapture(viewModel.task.id) }
  }

  private fun launchPhotoCapture(taskId: String) {
    try {
      val photoFile = userMediaRepository.createImageFile(taskId)
      capturedPhotoUri =
        FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID, photoFile)
      viewModel.taskWaitingForPhoto = taskId
      capturedPhotoPath = capturedPhotoUri.path
      capturePhotoLauncher.launch(capturedPhotoUri)
      Timber.d("Capture photo intent sent")
    } catch (e: IllegalArgumentException) {
      popups.ErrorPopup().unknownError()
      Timber.e(e)
    }
  }

  companion object {
    /** Key used to store field ID waiting for photo result across activity re-creation. */
    private const val TASK_WAITING_FOR_PHOTO = "dataCollectionPhotoFieldId"

    /** Key used to store captured photo Uri across activity re-creation. */
    private const val CAPTURED_PHOTO_PATH = "dataCollectionCapturedPhotoPath"
  }
}
