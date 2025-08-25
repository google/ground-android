/*
 * Copyright 2021 Google LLC
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
package org.groundplatform.android.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.groundplatform.android.common.Constants
import org.groundplatform.android.data.remote.RemoteStorageManager
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import timber.log.Timber

/**
 * Provides access to user-provided media stored locally and remotely. This currently includes only
 * photos.
 */
@Singleton
class UserMediaRepository
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val remoteStorageManager: RemoteStorageManager,
  private val uuidGenerator: OfflineUuidGenerator,
) {

  private var strFilePattern = Regex("^[a-zA-Z0-9._ -]+\\.(png|jpg)$")

  private val rootDir: File?
    get() = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

  private suspend fun createImageFilename(fieldId: String): String =
    fieldId + "-" + uuidGenerator.generateUuid() + Constants.PHOTO_EXT

  suspend fun createImageFile(fieldId: String): File = File(rootDir, createImageFilename(fieldId))

  /**
   * Creates a new file from bitmap and saves under external app directory.
   *
   * @throws IOException If path is not accessible or error occurs while saving file
   */
  @Throws(IOException::class)
  suspend fun savePhoto(bitmap: Bitmap, fieldId: String): File =
    createImageFile(fieldId).apply {
      FileOutputStream(this).use { fos -> bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos) }
      Timber.d("Photo saved %s : %b", path, exists())
    }

  /**
   * Saves the original image from the given [uri] to a file associated with the given [fieldId].
   *
   * This method copies the image bytes directly from the input stream without decoding it into
   * memory, preserving the original quality, metadata, and resolution. It avoids potential
   * OutOfMemoryError caused by loading large images into a Bitmap.
   */
  suspend fun savePhotoFromUri(uri: Uri, fieldId: String): File =
    withContext(Dispatchers.IO) {
      val file = createImageFile(fieldId)

      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
      } ?: throw IOException("Unable to open URI: $uri")

      Timber.d("Photo saved ${file.path} : ${file.exists()}")
      file
    }

  @Throws(FileNotFoundException::class)
  fun addImageToGallery(filePath: String, title: String): String =
    MediaStore.Images.Media.insertImage(context.contentResolver, filePath, title, "")

  /**
   * Attempts to load the file from local cache. Else attempts to fetch it from Firestore Storage.
   * Returns the uri of the file.
   *
   * @param path Final destination path of the uploaded file relative to Firestore
   */
  suspend fun getDownloadUrl(path: String?): Uri =
    if (path.isNullOrEmpty()) Uri.EMPTY else getFileUriFromRemotePath(path)

  private suspend fun getFileUriFromRemotePath(destinationPath: String): Uri {
    val file = getLocalFileFromRemotePath(destinationPath)
    return if (file.exists()) {
      Uri.fromFile(file)
    } else {
      Timber.d("File doesn't exist locally: %s", file.path)
      remoteStorageManager.getDownloadUrl(destinationPath)
    }
  }

  /**
   * Returns the path of the file saved in the sdcard used for uploading to the provided destination
   * path.
   */
  fun getLocalFileFromRemotePath(destinationPath: String): File {
    val filename = destinationPath.split('/').last()
    require(filename.matches(strFilePattern)) { "Invalid filename $filename" }
    val file = File(rootDir, filename)
    if (!file.exists()) {
      Timber.e("File not found: %s", file.path)
    }
    return file
  }
}
