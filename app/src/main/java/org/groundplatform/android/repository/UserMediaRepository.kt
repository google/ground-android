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
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import org.groundplatform.android.common.Constants
import org.groundplatform.android.data.remote.RemoteStorageManager
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.domain.repository.UserMediaRepositoryInterface
import org.groundplatform.domain.repository.UserMediaRepositoryInterface.MediaFilePath
import org.groundplatform.domain.repository.UserMediaRepositoryInterface.MediaUri
import timber.log.Timber

@Singleton
class UserMediaRepository
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val remoteStorageManager: RemoteStorageManager,
  private val uuidGenerator: OfflineUuidGenerator,
) : UserMediaRepositoryInterface {

  private var strFilePattern = Regex("^[a-zA-Z0-9._ -]+\\.(png|jpg)$")

  private val rootDir: File?
    get() = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

  private suspend fun createImageFilename(fieldId: String): String =
    fieldId + "-" + uuidGenerator.generateUuid() + Constants.PHOTO_EXT

  override suspend fun createImageFile(fieldId: String): MediaFilePath =
    File(rootDir, createImageFilename(fieldId)).toMediaFile()

  @Throws(FileNotFoundException::class)
  override fun addImageToGallery(filePath: String, title: String): String =
    MediaStore.Images.Media.insertImage(context.contentResolver, filePath, title, "")

  /**
   * Attempts to load the file from local cache. Else attempts to fetch it from Firestore Storage.
   * Returns the string uri of the file.
   *
   * @param path Final destination path of the uploaded file relative to Firestore
   */
  override suspend fun getDownloadUrl(path: String?): MediaUri =
    if (path.isNullOrEmpty()) "" else getFileUriFromRemotePath(path).toMediaUri()

  private suspend fun getFileUriFromRemotePath(destinationPath: String): Uri {
    val file = getLocalFileFromRemotePath(destinationPath).toFile()
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
  override fun getLocalFileFromRemotePath(destinationPath: String): MediaFilePath {
    val filename = destinationPath.split('/').last()
    require(filename.matches(strFilePattern)) { "Invalid filename $filename" }
    val file = File(rootDir, filename)
    if (!file.exists()) {
      Timber.e("File not found: %s", file.path)
    }
    return file.toMediaFile()
  }

  override fun getUriForFile(mediaFilePath: MediaFilePath): MediaUri =
    FileProvider.getUriForFile(
        context,
        org.groundplatform.android.BuildConfig.APPLICATION_ID,
        File(mediaFilePath),
      )
      .toMediaUri()

  private fun File.toMediaFile(): MediaFilePath = absolutePath

  private fun MediaFilePath.toFile(): File = File(this)

  private fun Uri.toMediaUri(): MediaUri = toString()
}
