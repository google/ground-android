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
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

class PhotoTaskViewModel
@Inject
constructor(
  private val userMediaRepository: UserMediaRepository,
  private val permissionsManager: PermissionsManager,
) : AbstractTaskViewModel() {

  private var tempPhotoFilePath: String? = null

  private val _isAwaitingPhotoCapture = MutableStateFlow(false)
  val isAwaitingPhotoCapture: StateFlow<Boolean> = _isAwaitingPhotoCapture.asStateFlow()

  private val _events = Channel<PhotoTaskEvent>()
  val events: Flow<PhotoTaskEvent> = _events.receiveAsFlow()

  val uri: StateFlow<Uri> =
    taskTaskData
      .map { taskData ->
        if (taskData is PhotoTaskData && taskData.isNotNullOrEmpty()) {
          userMediaRepository.getDownloadUrl(taskData.remoteFilename)
        } else {
          Uri.EMPTY
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Uri.EMPTY)

  fun onTakePhoto() {
    if (_isAwaitingPhotoCapture.value) return
    _isAwaitingPhotoCapture.value = true
    viewModelScope.launch { obtainCapturePhotoPermissions { launchPhotoCapture() } }
  }

  private suspend fun obtainCapturePhotoPermissions(onPermissionsGranted: suspend () -> Unit = {}) {
    try {
      if (Build.VERSION.SDK_INT < VERSION_CODES.R) {
        permissionsManager.obtainPermission(WRITE_EXTERNAL_STORAGE)
      }
      permissionsManager.obtainPermission(CAMERA)
      onPermissionsGranted()
    } catch (_: PermissionDeniedException) {
      _isAwaitingPhotoCapture.value = false
      _events.send(PhotoTaskEvent.ShowError(PhotoTaskError.PERMISSION_DENIED))
    }
  }

  private suspend fun launchPhotoCapture() {
    try {
      val file = userMediaRepository.createImageFile(task.id)
      tempPhotoFilePath = file.absolutePath
      val uri = userMediaRepository.getUriForFile(file)
      _events.send(PhotoTaskEvent.LaunchCamera(uri))
      Timber.d("Capture photo intent sent")
    } catch (e: IllegalArgumentException) {
      _isAwaitingPhotoCapture.value = false
      _events.send(PhotoTaskEvent.ShowError(PhotoTaskError.CAMERA_LAUNCH_FAILED))
      Timber.e(e, "Error launching photo capture")
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
    val filePath = tempPhotoFilePath
    tempPhotoFilePath = null // Clear to avoid reusing stale path
    viewModelScope.launch {
      if (result && filePath != null) {
        finalizePhotoCapture(File(filePath))
      } else {
        _events.send(PhotoTaskEvent.ShowError(PhotoTaskError.PHOTO_SAVE_FAILED))
      }
      _isAwaitingPhotoCapture.value = false
    }
  }

  /** Finalizes the photo capture by adding it to the gallery and updating the task data. */
  private suspend fun finalizePhotoCapture(file: File) {
    try {
      userMediaRepository.addImageToGallery(file.absolutePath, file.name)
      val remoteFilename = FirebaseStorageManager.getRemoteMediaPath(surveyId, file.name)
      setValue(PhotoTaskData(remoteFilename))
    } catch (e: Exception) {
      _events.send(PhotoTaskEvent.ShowError(PhotoTaskError.PHOTO_SAVE_FAILED))
      Timber.e(e, "Error finalizing photo capture")
    }
  }
}
