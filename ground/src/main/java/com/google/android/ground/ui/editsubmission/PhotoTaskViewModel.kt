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
package com.google.android.ground.ui.editsubmission

import android.Manifest.permission
import android.content.res.Resources
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import com.google.android.ground.model.submission.TextTaskData.Companion.fromString
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.firestore.FirestoreStorageManager.Companion.getRemoteMediaPath
import com.google.android.ground.repository.UserMediaRepository
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.ui.util.BitmapUtil
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

class PhotoTaskViewModel
@Inject
constructor(private val userMediaRepository: UserMediaRepository,
            private val permissionsManager: PermissionsManager,
            private val bitmapUtil: BitmapUtil,
            resources: Resources
) :
  AbstractTaskViewModel(resources) {

  // TODO(jsunde): Figure out how to return the result of the photo in the LiveData<Optional<TaskData>>

  // TODO(jsunde): This is currently duplicated here and in EditSubmissionViewModel.
  //  If it's here it can easily be used for the DataCollectionFragment, but is slightly more
  //  complicated to reuse from the EditSubmissionFragment. I should explore some more to see if
  //  it's possible to remove this logic from the EditSubmissionViewModel
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
  private var taskWaitingForPhoto: String? = null

  /**
   * Full path of the captured photo in local storage. In case of selecting a photo from storage,
   * URI is returned. But when capturing a photo using camera, we need to pass a valid URI and the
   * result returns true/false based on whether the operation passed or not. As only 1 photo result
   * is returned at a time, we can directly map it 1:1 with the path of the captured photo.
   */
  private var capturedPhotoPath: String? = null

  val uri: LiveData<Uri> =
    LiveDataReactiveStreams.fromPublisher(
      detailsTextFlowable.switchMapSingle { userMediaRepository.getDownloadUrl(it) }
    )

  val isPhotoPresent: LiveData<Boolean> =
    LiveDataReactiveStreams.fromPublisher(detailsTextFlowable.map { it.isNotEmpty() })

  private var surveyId: String? = null
  private var submissionId: String? = null

  private val showDialogClicks: @Hot(replays = true) MutableLiveData<Task> = MutableLiveData()
  private val editable: @Hot(replays = true) MutableLiveData<Boolean> = MutableLiveData(false)

  fun onShowPhotoSelectorDialog() {
    showDialogClicks.value = task
  }

  fun getShowDialogClicks(): LiveData<Task> = showDialogClicks

  fun setEditable(enabled: Boolean) {
    editable.postValue(enabled)
  }

  fun isEditable(): LiveData<Boolean> = editable

  fun updateResponse(value: String) {
    setResponse(fromString(value))
  }

  fun setSurveyId(surveyId: String?) {
    this.surveyId = surveyId
  }

  fun setSubmissionId(submissionId: String?) {
    this.submissionId = submissionId
  }

  fun onPhotoResult(photoResult: PhotoResult) {
    if (photoResult.isHandled) {
      return
    }
    if (surveyId == null || submissionId == null) {
      Timber.e("surveyId or submissionId not set")
      return
    }
    if (photoResult.taskId != task.id) {
      // Update belongs to another task.
      return
    }
    photoResult.isHandled = true
    if (photoResult.isEmpty()) {
      clearResponse()
      Timber.v("Photo cleared")
      return
    }
    try {
      val imageFile = getFileFromResult(photoResult)
      val filename = imageFile.name
      val path = imageFile.absolutePath

      // Add image to gallery.
      userMediaRepository.addImageToGallery(path, filename)

      // Update taskData.
      val remoteDestinationPath = getRemoteMediaPath(surveyId!!, submissionId!!, filename)
      updateResponse(remoteDestinationPath)
    } catch (e: IOException) {
      // TODO: Report error.
      Timber.e(e, "Failed to save photo")
    }
  }

  fun obtainCapturePhotoPermissions(): @Cold Completable {
    return permissionsManager
      .obtainPermission(permission.WRITE_EXTERNAL_STORAGE)
      .andThen(permissionsManager.obtainPermission(permission.CAMERA))
  }

  fun obtainSelectPhotoPermissions(): @Cold Completable {
    return permissionsManager.obtainPermission(permission.READ_EXTERNAL_STORAGE)
  }

  fun getTaskWaitingForPhoto(): String? {
    return taskWaitingForPhoto
  }

  fun setTaskWaitingForPhoto(taskWaitingForPhoto: String?) {
    this.taskWaitingForPhoto = taskWaitingForPhoto
  }

  fun getCapturedPhotoPath(): String? {
    return capturedPhotoPath
  }

  fun setCapturedPhotoPath(photoUri: String?) {
    this.capturedPhotoPath = photoUri
  }

  fun getLastPhotoResult(): Observable<PhotoResult?>? {
    return lastPhotoResult
  }

  fun onSelectPhotoResult(uri: Uri?) {
    if (uri == null) {
      Timber.v("Select photo failed or canceled")
      return
    }
    if (taskWaitingForPhoto == null) {
      Timber.e("Photo captured but no task waiting for the result")
      return
    }
    try {
      onPhotoResult(PhotoResult.createSelectResult(taskWaitingForPhoto, bitmapUtil.fromUri(uri)))
      Timber.v("Select photo result returned")
    } catch (e: IOException) {
      Timber.e(e, "Error getting photo selected from storage")
    }
  }

  @Throws(IOException::class)
  private fun getFileFromResult(result: PhotoResult): File {
    if (result.bitmap != null) {
      return userMediaRepository.savePhoto(result.bitmap, result.taskId)
    }
    if (result.path != null) {
      return File(result.path)
    }
    throw IllegalStateException("PhotoResult is empty")
  }
}
