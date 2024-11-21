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
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus.FAILED
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus.IN_PROGRESS
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus.PENDING
import com.google.android.ground.persistence.local.stores.LocalUserStore
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
  private val localUserStore: LocalUserStore,
  private val remoteDataStore: RemoteDataStore,
  private val mediaUploadWorkManager: MediaUploadWorkManager,
  private val userRepository: UserRepository,
) : CoroutineWorker(context, params) {

  private val locationOfInterestId: String =
    params.inputData.getString(LOCATION_OF_INTEREST_ID_PARAM_KEY)!!

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) { doWorkInternal() }

  private suspend fun doWorkInternal(): Result =
    try {
      val mutations = getIncompleteMutations()
      Timber.d("Syncing ${mutations.size} changes for LOI $locationOfInterestId")
      val result = processMutations(mutations)
      mediaUploadWorkManager.enqueueSyncWorker(locationOfInterestId)
      result
    } catch (t: Throwable) {
      Timber.e(t, "Failed to sync changes for LOI $locationOfInterestId")
      retry()
    }

  /**
   * Attempts to fetch all mutations from the [MutationRepository] that are `PENDING`, `FAILED`, or
   * `IN_PROGRESS` state. The latter should never occur since only on worker should be scheduled per
   * LOI at a given time.
   */
  private suspend fun getIncompleteMutations(): List<Mutation> =
    mutationRepository.getMutations(locationOfInterestId, PENDING, FAILED, IN_PROGRESS)

  /**
   * Applies mutations to remote data store. Once successful, removes them from the local db.
   *
   * @return `success` if the mutations were successfully synced with [RemoteDataStore].
   */
  private suspend fun processMutations(mutations: List<Mutation>): Result {
    if (mutations.isEmpty()) return success()
    return try {
      val user = getUserFromMutations(mutations)
      mutationRepository.markAsInProgress(mutations)
      remoteDataStore.applyMutations(mutations, user)
      mutationRepository.finalizePendingMutationsForMediaUpload(mutations)
      success()
    } catch (t: Throwable) {
      // Mark all mutations as having failed since the remote datastore only commits when all
      // mutations have succeeded.
      mutationRepository.markAsFailed(mutations, t)
      Timber.e(t, "Failed to sync survey ${mutations.first().surveyId}")
      retry()
    }
  }

  /** Returns a valid user associated with the mutations. */
  private suspend fun getUserFromMutations(mutations: List<Mutation>): User {
    val userIds = mutations.map { it.userId }.toSet()

    if (userIds.size != 1) {
      throw Error("Expected exactly 1 user, but found ${userIds.size}")
    }

    val userId = userIds.first()
    val loggedInUserId = userRepository.getAuthenticatedUser().id

    if (loggedInUserId != userId) {
      throw Error("Expected mutations for user '$loggedInUserId', but found '$userId'")
    }

    return localUserStore.getUserOrNull(userId)
      ?: throw Error("User removed before mutation processed")
  }

  companion object {
    internal const val LOCATION_OF_INTEREST_ID_PARAM_KEY = "locationOfInterestId"

    /** Returns a new work [Data] object containing the specified location of interest id. */
    @JvmStatic
    fun createInputData(locationOfInterestId: String): Data =
      Data.Builder().putString(LOCATION_OF_INTEREST_ID_PARAM_KEY, locationOfInterestId).build()
  }
}
