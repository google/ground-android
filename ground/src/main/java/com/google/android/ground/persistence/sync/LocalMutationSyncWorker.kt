/*
 * Copyright 2022 Google LLC
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
import androidx.work.ListenableWorker.Result.retry
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerParameters
import com.google.android.ground.model.User
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.submission.UploadQueueEntry
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.sync.LocalMutationSyncWorker.Companion.createInputData
import com.google.android.ground.repository.MutationRepository
import com.google.android.ground.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * A worker that syncs local changes to the remote data store. Each instance handles mutations for a
 * specific map location of interest, whose id is provided in the [Data] object built by
 * [createInputData] and provided to the worker request while being enqueued.
 */
@HiltWorker
class LocalMutationSyncWorker
@AssistedInject
constructor(
  @Assisted context: Context,
  @Assisted params: WorkerParameters,
  private val mutationRepository: MutationRepository,
  private val remoteDataStore: RemoteDataStore,
  private val mediaUploadWorkManager: MediaUploadWorkManager,
  private val userRepository: UserRepository,
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result =
    withContext(Dispatchers.IO) {
      val queue = mutationRepository.getPendingUploads()
      Timber.d("Uploading ${queue.size} additions / changes")
      if (queue.map { processQueueEntry(it) }.all { it }) success() else retry()
      // TODO: Update MediaUploader to work on entire queue, trigger when complete.
      //      mediaUploadWorkManager.enqueueSyncWorker(locationOfInterestId)
    }

  /**
   * Uploads a chunk of data to the remote data store, updating the upload status in the queue
   * accordingly.
   *
   * @return `true` if all data was uploaded, `false` if at least one failed.
   */
  private suspend fun processQueueEntry(entry: UploadQueueEntry): Boolean {
    val mutations = listOfNotNull(entry.loiMutation, entry.submissionMutation)
    return processMutations(mutations)
  }

  /**
   * Applies mutations to remote data store, updating their status in the queue accordingly. Catches
   * and handles all exceptions.
   *
   * @return `true` if all mutations were successfully synced with [RemoteDataStore], `false` if at
   *   least one failed.
   */
  private suspend fun processMutations(mutations: List<Mutation>): Boolean {
    if (mutations.isEmpty()) return true
    return try {
      val user = userRepository.getAuthenticatedUser()
      filterMutationsByUser(mutations, user)
        .takeIf { it.isNotEmpty() }
        ?.let {
          mutationRepository.markAsInProgress(it)
          remoteDataStore.applyMutations(it, user)
          mutationRepository.finalizePendingMutationsForMediaUpload(it)
        }
      true
    } catch (t: Throwable) {
      // Mark all mutations as having failed since the remote datastore only commits when all
      // mutations have succeeded.
      mutationRepository.markAsFailed(mutations, t)
      Timber.e(t, "Failed to sync survey ${mutations.first().surveyId}")
      false
    }
  }

  private fun filterMutationsByUser(mutations: List<Mutation>, user: User): List<Mutation> {
    val userIds = mutations.map { it.userId }.toSet()
    if (userIds.size != 1) {
      Timber.e("Expected exactly 1 user, but found ${userIds.size}")
    }
    val (validMutations, invalidMutations) = mutations.partition { it.userId == user.id }
    invalidMutations.forEach { Timber.e("Invalid mutation: $it") }
    return validMutations
  }

  companion object {
    internal const val LOCATION_OF_INTEREST_ID_PARAM_KEY = "locationOfInterestId"

    /** Returns a new work [Data] object containing the specified location of interest id. */
    @JvmStatic
    fun createInputData(locationOfInterestId: String): Data =
      Data.Builder().putString(LOCATION_OF_INTEREST_ID_PARAM_KEY, locationOfInterestId).build()
  }
}
