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
import androidx.work.ListenableWorker.Result.retry
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerParameters
import com.google.android.ground.model.User
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.repository.MutationRepository
import com.google.android.ground.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * A worker that uploads all pending local changes to the remote data store. Larger uploads (photos)
 * are then delegated to [MediaUploadWorkManager], which is enqueued and run in parallel.
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
      val queue = mutationRepository.getIncompleteUploads()
      Timber.d("Uploading ${queue.size} additions / changes")
      val results = queue.map { processMutations(it.mutations()) }
      if (results.any { it }) mediaUploadWorkManager.enqueueSyncWorker()
      if (results.all { it }) success() else retry()
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
      // TODO(https://github.com/google/ground-android/issues/2840):
      //   Move into repo for reuse by MediaUploadWorker.
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
}
