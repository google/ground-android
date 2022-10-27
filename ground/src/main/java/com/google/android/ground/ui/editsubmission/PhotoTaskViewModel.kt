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

import android.content.res.Resources
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import com.google.android.ground.model.submission.TextTaskData.Companion.fromString
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.firestore.FirestoreStorageManager.Companion.getRemoteMediaPath
import com.google.android.ground.repository.UserMediaRepository
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.editsubmission.EditSubmissionViewModel.PhotoResult
import java.io.File
import java.io.IOException
import javax.inject.Inject
import timber.log.Timber

class PhotoTaskViewModel
@Inject
constructor(private val userMediaRepository: UserMediaRepository, resources: Resources) :
  AbstractTaskViewModel(resources) {

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
    if (photoResult.isHandled()) {
      return
    }
    if (surveyId == null || submissionId == null) {
      Timber.e("surveyId or submissionId not set")
      return
    }
    if (!photoResult.hasTaskId(task.id)) {
      // Update belongs to another task.
      return
    }
    photoResult.setHandled(true)
    if (photoResult.isEmpty) {
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

  @Throws(IOException::class)
  private fun getFileFromResult(result: PhotoResult): File {
    if (result.bitmap.isPresent) {
      return userMediaRepository.savePhoto(result.bitmap.get(), result.taskId)
    }
    if (result.path.isPresent) {
      return File(result.path.get())
    }
    throw IllegalStateException("PhotoResult is empty")
  }
}
