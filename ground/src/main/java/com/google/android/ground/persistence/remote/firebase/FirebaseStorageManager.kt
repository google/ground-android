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
package com.google.android.ground.persistence.remote.firebase

import android.net.Uri
import com.google.android.ground.persistence.remote.RemoteStorageManager
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.StringJoiner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import timber.log.Timber

// TODO: Add column to Submission table for storing uploaded media urls
// TODO: Synced to remote db as well
@Singleton
class FirebaseStorageManager @Inject constructor() : RemoteStorageManager {

  @Inject lateinit var storageReference: StorageReference

  private fun createReference(path: String): StorageReference = storageReference.child(path)

  // StorageException's constructor logs errors, so even though we handle the exception,
  // an ERROR level log line is added which could be misleading to developers. We log an extra
  // error message here as an extra hint that the log line is probably noise.
  override suspend fun getDownloadUrl(remoteDestinationPath: String): Uri =
    createReference(remoteDestinationPath).downloadUrl.await()

  // Do not delete the file after successful upload. It is used as a cache
  // while viewing submissions when network is unavailable.
  override suspend fun uploadMediaFromFile(file: File, remoteDestinationPath: String) {
    try {
      createReference(remoteDestinationPath).putFile(Uri.fromFile(file)).await()
    } catch (e: CancellationException) {
      Timber.i(e, "Uploading media to remote storage cancelled")
    }
  }

  companion object {
    /** Top-level directory in Cloud Storage where user media is stored. */
    private const val MEDIA_ROOT_DIR = "user-media"

    /**
     * Generates destination path in which an submission attachment is to be stored in to Cloud
     * Storage.
     *
     * user-media/surveys/{survey_id}/submissions/{field_id-uuid.jpg}
     */
    // TODO: Refactor this into MediaStorageRepository.
    fun getRemoteMediaPath(surveyId: String, filename: String): String =
      StringJoiner(File.separator)
        .add(MEDIA_ROOT_DIR)
        .add("surveys")
        .add(surveyId)
        .add("submissions")
        .add(filename)
        .toString()
  }
}
