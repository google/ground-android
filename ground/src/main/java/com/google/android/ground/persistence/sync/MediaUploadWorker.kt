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
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.model.task.Task
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

  /**
   * Left-biased combination of two results. Failures propagate. Success will only be returned in
   * the case that both results are successful.
   */
  private fun <T> combineResults(a: kotlin.Result<T>, b: kotlin.Result<T>): kotlin.Result<T> =
    if (a.isSuccess) b else a

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) { doWorkInternal() }

  /** Performs media uploads for all eligible submission mutations associated with a given LOI. */
  private suspend fun doWorkInternal(): Result {
    check(loiId.isNotEmpty()) { "work was queued for an empty location of interest ID" }
    Timber.d("Starting media upload for LOI: $loiId")

    // Set of mutations eligible for media upload.
    val mutations =
      mutationRepository
        .getMutations(loiId, MutationEntitySyncStatus.MEDIA_UPLOAD_PENDING)
        .filterIsInstance<SubmissionMutation>()
        .filter { it.retryCount < Config.MAX_MEDIA_UPLOAD_RETRY_COUNT }
        .map {
          it.copy(
            syncStatus = Mutation.SyncStatus.MEDIA_UPLOAD_IN_PROGRESS,
            retryCount = it.retryCount + 1
          )
        }
    mutationRepository.updateMutations(mutations) // Mark MEDIA_UPLOAD_IN_PROGRESS

    val mutationStatusesAfterUpload = uploadMediaAndGetStatus(mutations)

    // The worker proceeds to a fixed point as follows:
    //   If there are any FAILED or PENDING mutations after the worker operation, we retry.
    //   During the retry, FAILED mutations will not be processed, while PENDING ones will only be
    //   reattempted if they satisfy the retry count bound. Eventually, either all of the
    //   mutations for this LOI will have succeeded, failed, or exceeded the retry count.
    if (
      mutationStatusesAfterUpload.any {
        it == Mutation.SyncStatus.FAILED || it == Mutation.SyncStatus.MEDIA_UPLOAD_PENDING
      }
    ) {
      return Result.retry()
    }
    return Result.success()
  }

  /**
   * Attempts to upload associated media for a given list of [SubmissionMutation] and updates the
   * status of each mutation based on whether uploads succeeded or failed.
   */
  private suspend fun uploadMediaAndGetStatus(
    mutations: List<SubmissionMutation>
  ): List<Mutation.SyncStatus> =
    mutations
      .map { uploadSubmissionMediaAndUpdateMutation(it) }
      .also { resultingMutations -> mutationRepository.updateMutations(resultingMutations) }
      .map { it.syncStatus }

  /**
   * Attempts to upload all media associated with a given submission. Updates the submission's sync
   * status depending on whether or the uploads failed or succeeded.
   */
  private suspend fun uploadSubmissionMediaAndUpdateMutation(
    mutation: SubmissionMutation
  ): SubmissionMutation =
    uploadSubmissionPhotos(mutation)
      .fold(
        onSuccess = { mutation.copy(syncStatus = Mutation.SyncStatus.COMPLETED) },
        onFailure = {
          val cause = it.message ?: "unknown upload error"
          if (it is FileNotFoundException) {
            return mutation.copy(syncStatus = Mutation.SyncStatus.FAILED, lastError = cause)
          }

          return mutation.copy(
            syncStatus = Mutation.SyncStatus.MEDIA_UPLOAD_PENDING,
            lastError = cause
          )
        }
      )

  private suspend fun uploadSubmissionPhotos(mutation: SubmissionMutation): kotlin.Result<Unit> =
    // TODO: Use media response types instead of discriminating on Task.Type.
    // For example, we should pass a List<PhotoResponse> to uploadPhotoMedia(), which can take care
    // of the bulk of the response-specific work.
    mutation.deltas
      .filter { (_, taskType, newValue): ValueDelta ->
        taskType === Task.Type.PHOTO && newValue.isNotNullOrEmpty()
      }
      .map { (_, _, newValue): ValueDelta -> newValue.toString() }
      .map { uploadPhotoMedia(it) }
      .reduce { a, b -> combineResults(a, b) }

  /**
   * Attempts to upload a single photo to remote storage. Returns an [UploadResult] indicating
   * whether the upload attempt failed or succeeded.
   */
  private suspend fun uploadPhotoMedia(path: String): kotlin.Result<Unit> {
    val photoFile = userMediaRepository.getLocalFileFromRemotePath(path)
    if (!photoFile.exists()) {
      Timber.e("Photo not found. local path: ${photoFile.path}, remote path: $path")
      return kotlin.Result.failure(
        FileNotFoundException("Photo $path not found on device: ${photoFile.path}")
      )
    }

    Timber.d("Starting photo upload. local path: ${photoFile.path}, remote path: $path")
    try {
      remoteStorageManager.uploadMediaFromFile(photoFile, path)
    } catch (e: Exception) {
      Timber.e("Photo upload failed. local path: ${photoFile.path}, remote path: $path", e)
      return kotlin.Result.failure(e)
    }

    return kotlin.Result.success(Unit)
  }

  companion object {
    private const val LOI_ID = "locationOfInterestId"

    @JvmStatic
    fun createInputData(locationOfInterestId: String): Data =
      Data.Builder().putString(LOI_ID, locationOfInterestId).build()
  }
}
