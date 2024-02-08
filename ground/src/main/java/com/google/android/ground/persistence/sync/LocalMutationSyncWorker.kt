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
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus.FAILED
import com.google.android.ground.model.mutation.Mutation.Type.CREATE
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.stores.LocalUserStore
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.sync.LocalMutationSyncWorker.Companion.createInputData
import com.google.android.ground.repository.MutationRepository
import com.google.android.ground.system.auth.AuthenticationManager
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
  private val authenticationManager: AuthenticationManager
) : CoroutineWorker(context, params) {

  private val locationOfInterestId: String =
    params.inputData.getString(LOCATION_OF_INTEREST_ID_PARAM_KEY)!!

  override suspend fun doWork(): Result =
    withContext(Dispatchers.IO) {
      try {
        // TODO: Move into new UploadUserDataUseCase class.
        val currentUser = authenticationManager.getAuthenticatedUser()
        val mutations = getEligibleMutations(currentUser)
        processMutations(mutations, currentUser)
      } catch (t: Throwable) {
        Timber.e(t, "Failed to sync changes for LOI $locationOfInterestId")
        retry()
      }
    }

  // TODO: Move into mutation repository.
  /**
   * Fetch mutations which are in `PENDING` state or in `FAILED` state but eligible for retry.
   * Ignores mutations not owned by the specified user.
   */
  private suspend fun getEligibleMutations(user: User): List<Mutation> {
    // TODO: Sort by mutation timestamp so that queue is FIFO.
    val pendingMutations =
      mutationRepository.getMutations(locationOfInterestId, MutationEntitySyncStatus.PENDING)
    val failedMutationsEligibleForRetry =
      mutationRepository
        .getMutations(locationOfInterestId, MutationEntitySyncStatus.FAILED)
        .filter { it.retryCount < MAX_RETRY_COUNT }

    val mutations = pendingMutations + failedMutationsEligibleForRetry
    val currentUsersMutations = mutations.filter { it.userId == user.id }
    if (mutations.size != currentUsersMutations.size) {
      Timber.w(
        "${mutations.size - currentUsersMutations.size} mutations found for " +
          "another user. These should have been removed at sign-out"
      )
    }
    return currentUsersMutations
  }

  private suspend fun processMutations(mutations: List<Mutation>, user: User): Result {
    Timber.v("Syncing ${mutations.size} changes for LOI $locationOfInterestId")

    // Split up create LOI mutations and other mutations.
    val (createLoiMutations, otherMutations) =
      mutations.filterIsInstance<LocationOfInterestMutation>().partition { it.type == CREATE }

    // All user data is associated with an LOI, so create those first if necessary. Stop and retry
    // if failed.
    if (!processCreateLoiMutations(createLoiMutations, user)) return retry()

    // Then process all other LOI and submission mutations.
    return if (processOtherMutations(otherMutations, user)) retry() else success()
  }

  private suspend fun processCreateLoiMutations(
    createLoiMutations: List<LocationOfInterestMutation>,
    user: User
  ): Boolean {
    if (createLoiMutations.size > 1)
      Timber.w("Duplicate create mutation found for LOI $locationOfInterestId")

    val createLoiMutation = createLoiMutations.lastOrNull()

    return createLoiMutation == null ||
      processMutation(createLoiMutation, user).syncStatus != FAILED
  }

  private suspend fun processOtherMutations(
    otherMutations: List<LocationOfInterestMutation>,
    user: User
  ): Boolean {
    val results = otherMutations.map { processMutation(it, user) }

    // Queue media worker if any submission mutations call for it.
    // TODO: Only call if any media uploads are pending.
    //    if (updatedMutations.any { it.syncStatus == Mutation.SyncStatus.MEDIA_UPLOAD_PENDING })
    mediaUploadWorkManager.enqueueSyncWorker(locationOfInterestId)

    return results.all { it }
  }

  /**
   * Applies mutations to remote data store. Once successful, removes them from the local db.
   *
   * @return `true` if the mutations were successfully synced with [RemoteDataStore].
   */
  private suspend fun processMutation(mutation: Mutation, user: User): Boolean {
    // TODO: Update methods to accept single mutation.
    val mutations = listOf(mutation)
    return try {
      mutationRepository.markAsInProgress(mutations)
      // TODO: Move to relevant repository.
      remoteDataStore.applyMutations(mutations, user)
      mutationRepository.finalizePendingMutationsForMediaUpload(mutations)
      true
    } catch (t: Throwable) {
      mutationRepository.markAsFailed(mutations, t)
      false
    }
  }

  companion object {
    // TODO: Move to Config class.
    private const val MAX_RETRY_COUNT = 10

    internal const val LOCATION_OF_INTEREST_ID_PARAM_KEY = "locationOfInterestId"

    /** Returns a new work [Data] object containing the specified location of interest id. */
    @JvmStatic
    fun createInputData(locationOfInterestId: String): Data =
      Data.Builder().putString(LOCATION_OF_INTEREST_ID_PARAM_KEY, locationOfInterestId).build()
  }
}
