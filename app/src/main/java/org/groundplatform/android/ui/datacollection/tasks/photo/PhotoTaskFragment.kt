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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.coroutines.ApplicationScope
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.coroutines.MainScope
import org.groundplatform.android.databinding.PhotoTaskFragBinding
import org.groundplatform.android.repository.UserMediaRepository
import org.groundplatform.android.system.PermissionDeniedException
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.TaskView
import org.groundplatform.android.ui.datacollection.components.TaskViewFactory
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.groundplatform.android.ui.home.HomeScreenViewModel
import org.groundplatform.android.util.renderComposableDialog
import timber.log.Timber

/** Fragment allowing the user to capture a photo to complete a task. */
@AndroidEntryPoint
class PhotoTaskFragment : AbstractTaskFragment<PhotoTaskViewModel>() {
  @Inject lateinit var userMediaRepository: UserMediaRepository
  @Inject @ApplicationScope lateinit var externalScope: CoroutineScope
  @Inject @MainScope lateinit var mainScope: CoroutineScope
  @Inject lateinit var permissionsManager: PermissionsManager
  @Inject lateinit var popups: EphemeralPopups
  @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
  lateinit var homeScreenViewModel: HomeScreenViewModel

  // Registers a callback to execute after a user captures a photo from the on-device camera.
  private lateinit var capturePhotoLauncher: ActivityResultLauncher<Uri>

  private var hasRequestedPermissionsOnResume = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    capturePhotoLauncher =
      registerForActivityResult(ActivityResultContracts.TakePicture()) { result: Boolean ->
        externalScope.launch(ioDispatcher) { viewModel.onCaptureResult(result) }
      }
  }

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val taskBinding = PhotoTaskFragBinding.inflate(inflater)
    taskBinding.lifecycleOwner = this
    taskBinding.fragment = this
    taskBinding.viewModel = viewModel
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)
    return taskBinding.root
  }

  override fun onTaskViewAttached() {
    viewModel.surveyId = dataCollectionViewModel.requireSurveyId()
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

  // Requests camera/photo access permissions from the device, executing an optional callback
  // when permission is granted.
  private fun obtainCapturePhotoPermissions(onPermissionsGranted: () -> Unit = {}) {
    lifecycleScope.launch {
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
        mainScope.launch { showPermissionDeniedDialog() }
      }
    }
  }

  private fun showPermissionDeniedDialog() {
    renderComposableDialog {
      ConfirmationDialog(
        title = R.string.permission_denied,
        description = R.string.camera_permissions_needed,
        confirmButtonText = R.string.ok,
        onConfirmClicked = {},
      )
    }
  }

  fun onTakePhoto() {
    if (viewModel.hasLaunchedCamera) return

    // Keep track of the fact that we are restoring the application after a photo capture.
    homeScreenViewModel.awaitingPhotoCapture = true
    obtainCapturePhotoPermissions { lifecycleScope.launch { launchPhotoCapture() } }
  }

  private suspend fun launchPhotoCapture() {
    try {
      viewModel.waitForPhotoCapture(viewModel.task.id)
      val uri = viewModel.createImageFileUri()
      viewModel.capturedUri = uri
      viewModel.hasLaunchedCamera = true
      capturePhotoLauncher.launch(viewModel.capturedUri)
      Timber.d("Capture photo intent sent")
    } catch (e: IllegalArgumentException) {
      homeScreenViewModel.awaitingPhotoCapture = false
      popups.ErrorPopup().unknownError()
      Timber.e(e)
    }
  }
}
