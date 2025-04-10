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
package org.groundplatform.android.persistence.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result.retry
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.FileNotFoundException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.model.mutation.SubmissionMutation
import org.groundplatform.android.model.submission.PhotoTaskData
import org.groundplatform.android.persistence.remote.RemoteStorageManager
import org.groundplatform.android.repository.MutationRepository
import org.groundplatform.android.repository.UserMediaRepository
import org.groundplatform.android.util.priority
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
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(context, workerParams) {

  override suspend fun doWork(): Result =
    withContext(ioDispatcher) {
      val mutations = mutationRepository.getIncompleteMediaMutations()
      Timber.d("Uploading photos for ${mutations.size} submission mutations")
      val results = mutations.map { uploadAllMedia(it) }
      if (results.all { it }) success() else retry()
    }

  /**
   * Upload all media associated with a given submission. Returns `true` if all uploads succeeds or
   * if there is nothing to upload, `false` otherwise.
   */
  private suspend fun uploadAllMedia(mutation: SubmissionMutation): Boolean {
    mutationRepository.markAsMediaUploadInProgress(listOf(mutation))
    val photoTasks = mutation.getPhotoData()
    val results = photoTasks.map { uploadPhotoMedia(it) }
    if (results.all { it.isSuccess }) {
      mutationRepository.markAsComplete(listOf(mutation))
      return true
    } else {
      mutationRepository.markAsFailedMediaUpload(
        listOf(mutation),
        // TODO: Replace this workaround with update of specific [MediaMutation],
        //  aggregate to [UploadQueueEntry] for display in UI.
        // Issue URL: https://github.com/google/ground-android/issues/2120
        results.firstNotNullOfOrNull { it.exceptionOrNull() } ?: UnknownError(),
      )
      return false
    }
  }

  /** Attempts to upload a single photo to remote storage. */
  private suspend fun uploadPhotoMedia(photoTaskData: PhotoTaskData): kotlin.Result<Unit> {
    try {
      val path = photoTaskData.remoteFilename
      val photoFile = userMediaRepository.getLocalFileFromRemotePath(path)
      Timber.d("Starting photo upload. local path: ${photoFile.path}, remote path: $path")
      if (!photoFile.exists()) {
        throw FileNotFoundException(photoFile.path)
      }
      remoteStorageManager.uploadMediaFromFile(photoFile, path)
      return kotlin.Result.success(Unit)
    } catch (t: Throwable) {
      Timber.log(t.priority(), t, "Photo upload failed")
      return kotlin.Result.failure(t)
    }
  }
}
