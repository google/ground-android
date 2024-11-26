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
package com.google.android.ground.persistence.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result.retry
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerParameters
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus.MEDIA_UPLOAD_AWAITING_RETRY
import com.google.android.ground.model.mutation.Mutation.SyncStatus.MEDIA_UPLOAD_IN_PROGRESS
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.PhotoTaskData
import com.google.android.ground.persistence.remote.RemoteStorageManager
import com.google.android.ground.repository.MutationRepository
import com.google.android.ground.repository.UserMediaRepository
import com.google.firebase.FirebaseNetworkException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * A worker that uploads media associated with submissions to the remote storage in the background.
 *
 * This worker should only run when the device has a network connection.
 *
 * The worker checks the local storage for any submission mutations that are ready for media upload.
 * If one or more media associated with the submission do not exist locally, the upload fails and is
 * marked as such. Otherwise, transient failures are considered eligible for retry.
 */
@HiltWorker
class MediaUploadWorker
@AssistedInject
constructor(
  @Assisted context: Context,
  @Assisted workerParams: WorkerParameters,
  private val remoteStorageManager: RemoteStorageManager,
  private val mutationRepository: MutationRepository,
  private val userMediaRepository: UserMediaRepository,
) : CoroutineWorker(context, workerParams) {

  override suspend fun doWork(): Result =
    withContext(Dispatchers.IO) {
      val uploadQueue = mutationRepository.getIncompleteMediaUploads()
      Timber.d("Uploading photos for ${uploadQueue.size} submission mutations")
      val results = uploadQueue.mapNotNull { it.submissionMutation }.map { uploadAllMedia(it) }
      if (results.all { it }) success() else retry()
    }

  /**
   * Upload all media associated with a given submission. Returns `true` if all uploads succeeds or
   * if there is nothing to upload, `false` otherwise.
   */
  private suspend fun uploadAllMedia(mutation: SubmissionMutation): Boolean {
    mutationRepository.saveMutationsLocally(
      listOf(mutation.updateSyncStatus(MEDIA_UPLOAD_IN_PROGRESS))
    )
    val photoTasks = mutation.deltas.map { it.newTaskData }.filterIsInstance<PhotoTaskData>()
    try {
      photoTasks.filter { !it.isEmpty() }.map { uploadPhotoMedia(it) }
      mutationRepository.saveMutationsLocally(
        listOf(mutation.updateSyncStatus(Mutation.SyncStatus.COMPLETED))
      )
      return true
    } catch (t: Throwable) {
      if (t is FirebaseNetworkException) {
        Timber.d(t, "Can't connect to Firebase to upload photo")
      } else {
        Timber.e(t, "Failed to upload photo")
      }
      // TODO: Clean up mutation update API in MutationRepository.
      mutationRepository.saveMutationsLocally(
        listOf(
          mutation.copy(
            syncStatus = MEDIA_UPLOAD_AWAITING_RETRY,
            lastError = t.message ?: "Unknown error",
            // TODO: Clean up handling of "retryCount" - "retry" doesn't mean retry now, it's means
            // "attempt". Also, the counter is unused now, and is set by both media and normal sync
            // worker.
            retryCount = mutation.retryCount + 1,
          )
        )
      )
      return false
    }
  }

  /** Attempts to upload a single photo to remote storage. */
  private suspend fun uploadPhotoMedia(photoTaskData: PhotoTaskData) {
    //    try {
    val path = photoTaskData.remoteFilename
    val photoFile = userMediaRepository.getLocalFileFromRemotePath(path)
    Timber.d("Starting photo upload. local path: ${photoFile.path}, remote path: $path")
    if (!photoFile.exists()) {
      throw FileNotFoundException(photoFile.path)
    }
    remoteStorageManager.uploadMediaFromFile(photoFile, path)
  }
  //      kotlin.Result.success(Unit)
  //    } catch (t: FirebaseNetworkException) {
  //      Timber.d(t, "Can't connect to Firebase to upload photo")
  //      kotlin.Result.failure(t)
  //    } catch (t: Throwable) {
  //      Timber.e(t, "Failed to upload photo")
  //      kotlin.Result.failure(t)
  //    }
}
