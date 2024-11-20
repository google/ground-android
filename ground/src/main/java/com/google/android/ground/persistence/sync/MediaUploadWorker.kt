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
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.android.ground.Config
import com.google.android.ground.FirebaseCrashLogger
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.PhotoTaskData
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.remote.RemoteStorageManager
import com.google.android.ground.repository.MutationRepository
import com.google.android.ground.repository.UserMediaRepository
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

  private val loiId: String = workerParams.inputData.getString(LOI_ID) ?: ""

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) { doWorkInternal() }

  /** Performs media uploads for all eligible submission mutations associated with a given LOI. */
  private suspend fun doWorkInternal(): Result {
    check(loiId.isNotEmpty()) { "work was queued for an empty location of interest ID" }
    Timber.d("Starting media upload for LOI: $loiId")

    val mutations =
      mutationRepository.getSubmissionMutations(
        loiId,
        MutationEntitySyncStatus.MEDIA_UPLOAD_PENDING,
        MutationEntitySyncStatus.MEDIA_UPLOAD_IN_PROGRESS,
        MutationEntitySyncStatus.MEDIA_UPLOAD_AWAITING_RETRY,
      )
    val results = uploadMedia(mutations)
    return if (results.any { it.mediaUploadPending() }) Result.retry() else Result.success()
  }

  /**
   * Attempts to upload associated media for a given list of [SubmissionMutation] and updates the
   * status of each mutation based on whether uploads succeeded or failed.
   */
  private suspend fun uploadMedia(mutations: List<SubmissionMutation>): List<SubmissionMutation> =
    mutations
      .map { mutation ->
        mutation
          .updateSyncStatus(Mutation.SyncStatus.MEDIA_UPLOAD_IN_PROGRESS)
          .incrementRetryCount()
      }
      .also { mutationRepository.saveMutationsLocally(it) }
      .map { mutation -> uploadMedia(mutation) }
      .also { mutationRepository.saveMutationsLocally(it) }

  /**
   * Attempts to upload all media associated with a given submission. Updates the submission's sync
   * status depending on whether or the uploads failed or succeeded.
   */
  private suspend fun uploadMedia(mutation: SubmissionMutation): SubmissionMutation {
    val photoTasks = mutation.deltas.map { it.newTaskData }.filterIsInstance<PhotoTaskData>()
    return uploadPhotos(photoTasks)
      .fold(
        onSuccess = { mutation.updateSyncStatus(Mutation.SyncStatus.COMPLETED) },
        onFailure = {
          return mutation.copy(
            syncStatus =
              if (it is FileNotFoundException || !mutation.canRetry()) {
                Mutation.SyncStatus.FAILED
              } else {
                Mutation.SyncStatus.MEDIA_UPLOAD_AWAITING_RETRY
              },
            lastError = it.message ?: "unknown upload error",
          )
        },
      )
  }

  private suspend fun uploadPhotos(photoTaskDataList: List<PhotoTaskData>): kotlin.Result<Unit> =
    // TODO(#2120): Retry uploads on a per-photo basis, instead of per-response.
    photoTaskDataList
      .filter { !it.isEmpty() }
      .map { uploadPhotoMedia(it) }
      .fold(kotlin.Result.success(Unit)) { a, b -> if (a.isSuccess) b else a }

  /**
   * Attempts to upload a single photo to remote storage. Returns an [Result] indicating whether the
   * upload attempt failed or succeeded.
   */
  private suspend fun uploadPhotoMedia(photoTaskData: PhotoTaskData): kotlin.Result<Unit> {
    val path = photoTaskData.remoteFilename
    val photoFile = userMediaRepository.getLocalFileFromRemotePath(path)
    if (!photoFile.exists()) {
      Timber.e("Photo not found. local path: ${photoFile.path}, remote path: $path")
      return kotlin.Result.failure(
        FileNotFoundException("Photo $path not found on device: ${photoFile.path}")
      )
    }

    Timber.d("Starting photo upload. local path: ${photoFile.path}, remote path: $path")
    return try {
      remoteStorageManager.uploadMediaFromFile(photoFile, path)
      kotlin.Result.success(Unit)
    } catch (t: Throwable) {
      Timber.e("Photo upload failed. local path: ${photoFile.path}, remote path: $path", t)
      FirebaseCrashLogger().logException(t)
      kotlin.Result.failure(t)
    }
  }

  companion object {
    private const val LOI_ID = "locationOfInterestId"

    @JvmStatic
    fun createInputData(locationOfInterestId: String): Data =
      Data.Builder().putString(LOI_ID, locationOfInterestId).build()

    private fun SubmissionMutation.canRetry() =
      this.retryCount < Config.MAX_MEDIA_UPLOAD_RETRY_COUNT
  }
}
