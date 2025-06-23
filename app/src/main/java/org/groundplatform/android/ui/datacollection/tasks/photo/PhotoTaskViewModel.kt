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

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.groundplatform.android.coroutines.ApplicationScope
import org.groundplatform.android.model.submission.PhotoTaskData
import org.groundplatform.android.model.submission.isNotNullOrEmpty
import org.groundplatform.android.persistence.remote.firebase.FirebaseStorageManager
import org.groundplatform.android.repository.UserMediaRepository
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskViewModel
import timber.log.Timber

class PhotoTaskViewModel
@Inject
constructor(
  private val userMediaRepository: UserMediaRepository,
  @ApplicationScope private val externalScope: CoroutineScope,
) : AbstractTaskViewModel() {

  /**
   * Task id waiting for a photo result. As only one photo result is returned at a time, we can
   * directly map it 1:1 with the task waiting for a photo result.
   */
  var taskWaitingForPhoto: String? = null

  lateinit var surveyId: String

  val uri: LiveData<Uri> =
    taskTaskData
      .filterIsInstance<PhotoTaskData>()
      .map { it.remoteFilename }
      .map { userMediaRepository.getDownloadUrl(it) }
      .asLiveData()

  val isPhotoPresent: LiveData<Boolean> = taskTaskData.map { it.isNotNullOrEmpty() }.asLiveData()

  /**
   * Saves photo data stored on an on-device URI in Ground-associated storage and prepares it for
   * inclusion in a data collection submission.
   */
  fun savePhotoTaskData(uri: Uri) {
    val currentTask = taskWaitingForPhoto
    requireNotNull(currentTask) { "Photo captured but no task waiting for the result" }
    externalScope.launch(Dispatchers.IO) {
      try {
        val file = userMediaRepository.savePhotoFromUri(uri, currentTask)
        userMediaRepository.addImageToGallery(file.absolutePath, file.name)
        val remoteFilename = FirebaseStorageManager.getRemoteMediaPath(surveyId, file.name)

        withContext(Dispatchers.Main) { setValue(PhotoTaskData(remoteFilename)) }
      } catch (e: IOException) {
        Timber.e(e, "Error getting photo selected from storage")
      } catch (e: Exception) {
        Timber.e(e, "Unexpected error processing photo")
      }
    }
  }
}
