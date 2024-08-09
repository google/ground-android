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
import com.google.android.ground.model.submission.PhotoTaskData
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.persistence.remote.firebase.FirebaseStorageManager
import com.google.android.ground.repository.UserMediaRepository
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.util.BitmapUtil
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class PhotoTaskViewModel
@Inject
constructor(
  private val userMediaRepository: UserMediaRepository,
  private val bitmapUtil: BitmapUtil,
  resources: Resources,
) : AbstractTaskViewModel(resources) {

  /**
   * Task id waiting for a photo result. As only one photo result is returned at a time, we can
   * directly map it 1:1 with the task waiting for a photo result.
   */
  var taskWaitingForPhoto: String? = null

  lateinit var surveyId: String

  val uri: LiveData<Uri> =
    taskTaskData.map { userMediaRepository.getDownloadUrl(it?.getDetailsText()) }.asLiveData()
  val isPhotoPresent: LiveData<Boolean> = taskTaskData.map { it.isNotNullOrEmpty() }.asLiveData()

  /**
   * Saves photo data stored on an on-device URI in Ground-associated storage and prepares it for
   * inclusion in a data collection submission.
   */
  fun savePhotoTaskData(uri: Uri) {
    val currentTask = taskWaitingForPhoto
    requireNotNull(currentTask) { "Photo captured but no task waiting for the result" }

    try {
      val bitmap = bitmapUtil.fromUri(uri)
      val file = userMediaRepository.savePhoto(bitmap, currentTask)
      userMediaRepository.addImageToGallery(file.absolutePath, file.name)
      val remoteFilename = FirebaseStorageManager.getRemoteMediaPath(surveyId, file.absolutePath)
      setValue(PhotoTaskData(remoteFilename))
    } catch (e: IOException) {
      Timber.e(e, "Error getting photo selected from storage")
    }
  }
}
