/*
 * Copyright 2020 Google LLC
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

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.groundplatform.android.R
import org.groundplatform.android.data.remote.firebase.FirebaseStorageManager
import org.groundplatform.android.repository.UserMediaRepository
import org.groundplatform.android.system.PermissionDeniedException
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskViewModel
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.submission.isNotNullOrEmpty
import org.groundplatform.domain.model.task.PhotoTaskData
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class PhotoTaskViewModel
@Inject
constructor(
  private val userMediaRepository: UserMediaRepository,
  private val permissionsManager: PermissionsManager,
) : AbstractTaskViewModel() {

  private val _isAwaitingPhotoCapture = MutableStateFlow(false)
  val isAwaitingPhotoCapture: StateFlow<Boolean> = _isAwaitingPhotoCapture.asStateFlow()

  /**
   * Task id waiting for a photo result. As only one photo result is returned at a time, we can
   * directly map it 1:1 with the task waiting for a photo result.
   */
  var taskWaitingForPhoto: String? = null

  var capturedUri: Uri? = null

  private val _events = MutableSharedFlow<PhotoTaskEvent>()
  val events: SharedFlow<PhotoTaskEvent> = _events.asSharedFlow()

  fun onTakePhoto() {
    if (_isAwaitingPhotoCapture.value) return

    viewModelScope.launch {
      _isAwaitingPhotoCapture.value = true
      obtainCapturePhotoPermissions { launchPhotoCapture() }
    }
  }

  suspend fun obtainCapturePhotoPermissions(onPermissionsGranted: suspend () -> Unit = {}) {
    try {
      if (Build.VERSION.SDK_INT < VERSION_CODES.R) {
        permissionsManager.obtainPermission(WRITE_EXTERNAL_STORAGE)
      }
      permissionsManager.obtainPermission(CAMERA)
      onPermissionsGranted()
    } catch (_: PermissionDeniedException) {
      _events.emit(
        PhotoTaskEvent.ShowError(R.string.permission_denied, R.string.camera_permissions_needed)
      )
    }
  }

  private suspend fun launchPhotoCapture() {
    try {
      taskWaitingForPhoto = task.id
      val file = userMediaRepository.createImageFile(task.id)
      val uri = userMediaRepository.getUriForFile(file)
      capturedUri = uri
      _events.emit(PhotoTaskEvent.LaunchCamera(uri))
      Timber.d("Capture photo intent sent")
    } catch (e: IllegalArgumentException) {
      _isAwaitingPhotoCapture.value = false
      _events.emit(PhotoTaskEvent.ShowError(R.string.unexpected_error, R.string.unexpected_error))
      Timber.e(e, "Error launching photo capture")
    }
  }

  val uri: Flow<Uri> = taskTaskData.map { taskData ->
    if (taskData is PhotoTaskData && taskData.isNotNullOrEmpty()) {
      userMediaRepository.getDownloadUrl(taskData.remoteFilename)
    } else {
      Uri.EMPTY
    }
  }

  override fun getButtonStates(taskData: TaskData?): List<ButtonActionState> =
    listOf(
      getPreviousButton(),
      getUndoButton(taskData),
      getSkipButton(taskData),
      getNextButton(taskData),
    )

  fun onCaptureResult(result: Boolean) {
    if (result && capturedUri != null) {
      viewModelScope.launch { savePhotoTaskData(capturedUri!!) }
    }
    _isAwaitingPhotoCapture.value = false
  }

  /**
   * Saves photo data stored on an on-device URI in Ground-associated storage and prepares it for
   * inclusion in a data collection submission.
   */
  private suspend fun savePhotoTaskData(uri: Uri) {
    val currentTask = taskWaitingForPhoto
    requireNotNull(currentTask) { "Photo captured but no task waiting for the result" }

    try {
      val file = userMediaRepository.savePhotoFromUri(uri, currentTask)
      userMediaRepository.addImageToGallery(file.absolutePath, file.name)
      val remoteFilename = FirebaseStorageManager.getRemoteMediaPath(surveyId, file.name)

      withContext(Dispatchers.Main) { setValue(PhotoTaskData(remoteFilename)) }
    } catch (e: IOException) {
      Timber.e(e, "Error saving photo to storage")
      _events.emit(PhotoTaskEvent.ShowError(R.string.unexpected_error, R.string.unexpected_error))
    }
  }
}
