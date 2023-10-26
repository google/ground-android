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
package com.google.android.ground.ui.datacollection.tasks.photo

import android.content.res.Resources
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.google.android.ground.model.submission.TextTaskData.Companion.fromString
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.persistence.remote.firebase.FirebaseStorageManager.Companion.getRemoteMediaPath
import com.google.android.ground.repository.UserMediaRepository
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.util.BitmapUtil
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import timber.log.Timber

class PhotoTaskViewModel
@Inject
constructor(
  private val userMediaRepository: UserMediaRepository,
  private val bitmapUtil: BitmapUtil,
  resources: Resources
) : AbstractTaskViewModel(resources) {

  /**
   * Emits the last photo task id updated and either its photo result, or empty if removed. The last
   * value is emitted on each subscription because {@see #onPhotoResult} is called before
   * subscribers are created.
   */
  private val lastPhotoResult: Subject<PhotoResult> = BehaviorSubject.create()

  /**
   * Task id waiting for a photo taskData. As only 1 photo result is returned at a time, we can
   * directly map it 1:1 with the task waiting for a photo taskData.
   */
  var taskWaitingForPhoto: String? = null

  /**
   * Full path of the captured photo in local storage. In case of selecting a photo from storage,
   * URI is returned. But when capturing a photo using camera, we need to pass a valid URI and the
   * result returns true/false based on whether the operation passed or not. As only 1 photo result
   * is returned at a time, we can directly map it 1:1 with the path of the captured photo.
   */
  var capturedPhotoPath: String? = null

  lateinit var surveyId: String

  val uri: LiveData<Uri> =
    taskDataValue.map { userMediaRepository.getDownloadUrl(it?.getDetailsText()) }.asLiveData()

  val isPhotoPresent: LiveData<Boolean> = taskDataValue.map { it.isNotNullOrEmpty() }.asLiveData()

  fun onPhotoResult(photoResult: PhotoResult) {
    if (photoResult.isHandled) {
      return
    }
    if (photoResult.taskId != task.id) {
      // Update belongs to another task.
      return
    }
    if (photoResult.isEmpty()) {
      clearResponse()
      Timber.v("Photo cleared")
      return
    }
    try {
      val imageFile = getFileFromResult(photoResult.copy(isHandled = true))
      val filename = imageFile.name
      val path = imageFile.absolutePath

      // Add image to gallery.
      userMediaRepository.addImageToGallery(path, filename)

      // Update taskData.
      val remoteDestinationPath = getRemoteMediaPath(surveyId, filename)
      setResponse(fromString(remoteDestinationPath))
    } catch (e: IOException) {
      // TODO: Report error.
      Timber.e(e, "Failed to save photo")
    }
  }

  fun getLastPhotoResult(): Observable<PhotoResult?> = lastPhotoResult

  fun onSelectPhotoResult(uri: Uri?) {
    if (uri == null) {
      Timber.v("Select photo failed or canceled")
      return
    }
    val currentTask = taskWaitingForPhoto
    if (currentTask == null) {
      Timber.e("Photo captured but no task waiting for the result")
      return
    }
    try {
      onPhotoProvided(PhotoResult(currentTask, bitmapUtil.fromUri(uri)))
      Timber.v("Select photo result returned")
    } catch (e: IOException) {
      Timber.e(e, "Error getting photo selected from storage")
    }
  }

  fun onCapturePhotoResult(result: Boolean) {
    if (!result) {
      Timber.v("Capture photo failed or canceled")
      // TODO: Cleanup created file if it exists.
      return
    }
    val currentTask = taskWaitingForPhoto
    if (currentTask == null) {
      Timber.e("Photo captured but no task waiting for the result")
      return
    }
    if (capturedPhotoPath == null) {
      Timber.e("Photo captured but no path available to read the result")
      return
    }
    onPhotoProvided(PhotoResult(currentTask, /* bitmap=*/ null, capturedPhotoPath))
    Timber.v("Photo capture result returned")
  }

  private fun onPhotoProvided(result: PhotoResult) {
    capturedPhotoPath = null
    taskWaitingForPhoto = null
    lastPhotoResult.onNext(result)
  }

  @Throws(IOException::class)
  private fun getFileFromResult(result: PhotoResult): File {
    if (result.bitmap != null) {
      return userMediaRepository.savePhoto(result.bitmap, result.taskId)
    }
    if (result.path != null) {
      Timber.d("Photo saved %s : %b", result.path, File(result.path).exists())
      return File(result.path)
    }

    error("PhotoResult is empty")
  }
}
