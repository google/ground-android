/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.domain.repository

/**
 * Provides access to user-provided media stored locally and remotely. This currently includes only
 * photos.
 */
interface UserMediaRepositoryInterface {
  typealias MediaUri = String

  typealias MediaFilePath = String

  suspend fun createImageFile(fieldId: String): MediaFilePath

  fun addImageToGallery(filePath: String, title: String): String

  /**
   * Attempts to load the file from local cache. Else attempts to fetch it from Firestore Storage.
   * Returns the uri of the file.
   *
   * @param path Final destination path of the uploaded file relative to Firestore
   */
  suspend fun getDownloadUrl(path: String?): MediaUri

  /**
   * Returns the path of the file saved in the sdcard used for uploading to the provided destination
   * path.
   */
  fun getLocalFileFromRemotePath(destinationPath: String): MediaFilePath

  fun getUriForFile(mediaFilePath: MediaFilePath): MediaUri
}
