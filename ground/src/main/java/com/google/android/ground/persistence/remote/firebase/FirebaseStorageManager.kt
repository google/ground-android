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
import com.google.android.ground.rx.RxTask
import com.google.android.ground.rx.annotations.Cold
import com.google.firebase.storage.StorageReference
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.Single
import java.io.File
import java8.util.StringJoiner
import javax.inject.Inject
import javax.inject.Singleton
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
  override fun getDownloadUrl(remoteDestinationPath: String): @Cold Single<Uri> =
    RxTask.toSingle { createReference(remoteDestinationPath).downloadUrl }
      .doOnError { Timber.e(it, "StorageException handled and can be ignored") }

  // Do not delete the file after successful upload. It is used as a cache
  // while viewing submissions when network is unavailable.
  override fun uploadMediaFromFile(file: File, remoteDestinationPath: String): @Cold Completable =
    Completable.create { emitter: CompletableEmitter ->
      createReference(remoteDestinationPath)
        .putFile(Uri.fromFile(file))
        .addOnCompleteListener { emitter.onComplete() }
        .addOnFailureListener { emitter.onError(it) }
    }

  companion object {
    /** Top-level directory in Cloud Storage where user media is stored. */
    private const val MEDIA_ROOT_DIR = "user-media"

    /**
     * Generates destination path in which an submission attachment is to be stored in to Cloud
     * Storage.
     *
     * user-media/surveys/{survey_id}/submissions/{submission_id}/{field_id-uuid.jpg}
     */
    // TODO: Refactor this into MediaStorageRepository.
    @JvmStatic
    fun getRemoteMediaPath(surveyId: String, submissionId: String, filename: String): String =
      StringJoiner(File.separator)
        .add(MEDIA_ROOT_DIR)
        .add("surveys")
        .add(surveyId)
        .add("submissions")
        .add(submissionId)
        .add(filename)
        .toString()
  }
}
