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
package org.groundplatform.android.persistence.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result.retry
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.persistence.remote.RemoteDataStore
import org.groundplatform.android.repository.MutationRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.util.priority
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
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result =
    withContext(ioDispatcher) {
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
    try {
      val user = userRepository.getAuthenticatedUser()
      mutationRepository.markAsInProgress(mutations)
      // TODO: Apply mutations via repository layer rather than accessing data store directly.
      // Issue URL: https://github.com/google/ground-android/issues/2883
      remoteDataStore.applyMutations(mutations, user)
      mutationRepository.finalizePendingMutationsForMediaUpload(mutations)

      return true
    } catch (t: Throwable) {
      // Mark all mutations as having failed since the remote datastore only commits when all
      // mutations have succeeded.
      mutationRepository.markAsFailed(mutations, t)
      Timber.log(t.priority(), t, "Failed to sync local data")
      return false
    }
  }
}
