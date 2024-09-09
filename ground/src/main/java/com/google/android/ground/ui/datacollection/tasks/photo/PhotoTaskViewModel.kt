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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.google.android.ground.model.submission.PhotoTaskData
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.persistence.remote.firebase.FirebaseStorageManager
import com.google.android.ground.repository.UserMediaRepository
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.util.BitmapUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import timber.log.Timber

class PhotoTaskViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val userMediaRepository: UserMediaRepository,
  private val bitmapUtil: BitmapUtil,
) : AbstractTaskViewModel() {

  /**
   * Task id waiting for a photo result. As only one photo result is returned at a time, we can
   * directly map it 1:1 with the task waiting for a photo result.
   */
  var taskWaitingForPhoto: String? = null

  lateinit var surveyId: String

  val uri: LiveData<Uri> =
    taskTaskData.map { userMediaRepository.getDownloadUrl(it?.getDetailsText()) }.asLiveData()
  val isPhotoPresent: LiveData<Boolean> = taskTaskData.map { it.isNotNullOrEmpty() }.asLiveData()

  private fun rotateBitmap(bitmap: Bitmap, rotateDegrees: Float): Bitmap {
    val matrix = Matrix()
    // Rotate iff rotation is non-zero.
    if (rotateDegrees != 0f) {
      matrix.postRotate(rotateDegrees)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true)
  }

  /**
   * Saves photo data stored on an on-device URI in Ground-associated storage and prepares it for
   * inclusion in a data collection submission.
   */
  fun savePhotoTaskData(uri: Uri) {
    val currentTask = taskWaitingForPhoto
    requireNotNull(currentTask) { "Photo captured but no task waiting for the result" }

    try {
      val orientation = getOrientationFromExif(uri)
      val rotateDegrees = getRotationDegrees(orientation)
      val bitmap = rotateBitmap(bitmapUtil.fromUri(uri), rotateDegrees)
      val file = userMediaRepository.savePhoto(bitmap, currentTask)
      userMediaRepository.addImageToGallery(file.absolutePath, file.name)
      val remoteFilename = FirebaseStorageManager.getRemoteMediaPath(surveyId, file.name)
      setValue(PhotoTaskData(remoteFilename))
    } catch (e: IOException) {
      Timber.e(e, "Error getting photo selected from storage")
    }
  }

  /**
   * Returns the number of degrees a photo should be rotated based on the value of its orientation
   * EXIF tag.
   */
  private fun getRotationDegrees(orientation: Int): Float =
    when (orientation) {
      ExifInterface.ORIENTATION_UNDEFINED,
      ExifInterface.ORIENTATION_NORMAL -> 0f
      ExifInterface.ORIENTATION_ROTATE_90 -> 90f
      ExifInterface.ORIENTATION_ROTATE_180 -> 180f
      ExifInterface.ORIENTATION_ROTATE_270 -> 270f
      else -> throw UnsupportedOperationException("Unsupported photo orientation $orientation")
    }

  /** Returns the EXIF orientation attribute of the JPEG image at the specified URI. */
  private fun getOrientationFromExif(uri: Uri): Int {
    val inputStream =
      context.contentResolver.openInputStream(uri)
        ?: throw IOException("Content resolver returned null for $uri")
    val exif = ExifInterface(inputStream)
    return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
  }
}
