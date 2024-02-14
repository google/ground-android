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
  private val userRepository: UserRepository,
  private val remoteDataStore: RemoteDataStore,
  private val mediaUploadWorkManager: MediaUploadWorkManager
) : CoroutineWorker(context, params) {

  private val locationOfInterestId: String =
    params.inputData.getString(LOCATION_OF_INTEREST_ID_PARAM_KEY)!!

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) { doWorkInternal() }

  private suspend fun doWorkInternal(): Result =
    try {
      val user = userRepository.getAuthenticatedUser()
      val mutations =
        mutationRepository.getMutationsEligibleForRetry(
          locationOfInterestId,
          user.id,
          MAX_RETRY_COUNT
        )
      Timber.d(
        "Syncing ${mutations.size} changes authored by user ${user.id} on LOI $locationOfInterestId"
      )
      val result = processMutations(mutations, user)
      mediaUploadWorkManager.enqueueSyncWorker(locationOfInterestId)
      if (result) success() else retry()
    } catch (t: Throwable) {
      Timber.e(t, "Failed to sync changes for LOI $locationOfInterestId")
      retry()
    }

  /**
   * Applies mutations to remote data store. Once successful, removes them from the local db.
   *
   * @return `true` if the mutations were successfully synced with [RemoteDataStore].
   */
  private suspend fun processMutations(mutations: List<Mutation>, user: User): Boolean {
    // TODO(#2235): Process and update each mutation individually.
    if (mutations.isEmpty()) return true

    return try {
      mutationRepository.markAsInProgress(mutations)
      remoteDataStore.applyMutations(mutations, user)
      mutationRepository.finalizePendingMutationsForMediaUpload(mutations)
      true
    } catch (t: Throwable) {
      mutationRepository.markAsFailed(mutations, t)
      false
    }
  }

  companion object {
    private const val MAX_RETRY_COUNT = 10

    internal const val LOCATION_OF_INTEREST_ID_PARAM_KEY = "locationOfInterestId"

    /** Returns a new work [Data] object containing the specified location of interest id. */
    @JvmStatic
    fun createInputData(locationOfInterestId: String): Data =
      Data.Builder().putString(LOCATION_OF_INTEREST_ID_PARAM_KEY, locationOfInterestId).build()
  }
}
