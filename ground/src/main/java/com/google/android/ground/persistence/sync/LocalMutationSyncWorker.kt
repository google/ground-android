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
import androidx.work.WorkerParameters
import com.google.android.ground.model.User
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.stores.LocalUserStore
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.sync.LocalMutationSyncWorker.Companion.createInputData
import com.google.android.ground.repository.MutationRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
  private val photoSyncWorkManager: PhotoSyncWorkManager
) : CoroutineWorker(context, params) {

  private val locationOfInterestId: String =
    params.inputData.getString(LOCATION_OF_INTEREST_ID_PARAM_KEY)!!

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) { doWorkInternal() }

  private suspend fun doWorkInternal(): Result {
    Timber.d("Connected. Syncing changes to location of interest $locationOfInterestId")
    val mutations: List<Mutation> = mutationRepository.getPendingMutations(locationOfInterestId)
    return try {
      Timber.v("Mutations: $mutations")
      processMutations(mutations)
      Result.success()
    } catch (t: Throwable) {
      FirebaseCrashlytics.getInstance()
        .log("Error applying remote updates to location of interest $locationOfInterestId")
      FirebaseCrashlytics.getInstance().recordException(t)
      Timber.e(t, "Remote updates for location of interest $locationOfInterestId failed")
      mutationRepository.updateMutations(incrementRetryCounts(mutations, t))
      Result.retry()
    }
  }

  /**
   * Groups mutations by user id, loads each user, applies mutations, and removes processed
   * mutations.
   */
  private suspend fun processMutations(pendingMutations: List<Mutation>) {
    val mutationsByUserId: Map<String, List<Mutation>> = groupByUserId(pendingMutations)
    val userIds = mutationsByUserId.keys
    for (userId in userIds) {
      val mutations = mutationsByUserId[userId] ?: listOf()
      processMutations(mutations, userId)
    }
  }

  /** Loads each user with specified id, applies mutations, and removes processed mutations. */
  private suspend fun processMutations(mutations: List<Mutation>, userId: String) {
    try {
      val user = localUserStore.getUser(userId)
      processMutations(mutations, user)
    } catch (e: Exception) {
      Timber.e(e, "User account removed before mutation processed")
    }
  }

  /** Applies mutations to remote data store. Once successful, removes them from the local db. */
  private suspend fun processMutations(mutations: List<Mutation>, user: User) {
    remoteDataStore.applyMutations(mutations, user)
    processPhotoFieldMutations(mutations)
    // TODO: If the remote sync fails, reset the state to DEFAULT.
    mutationRepository.finalizePendingMutations(mutations)
  }

  /**
   * Filters all mutations containing submission mutations with changes to photo fields and uploads
   * to remote storage.
   */
  private fun processPhotoFieldMutations(mutations: List<Mutation>) =
    mutations
      .filterIsInstance<SubmissionMutation>()
      .flatMap { mutation: Mutation -> (mutation as SubmissionMutation).taskDataDeltas }
      .filter { (_, taskType, newResponse): TaskDataDelta ->
        taskType === Task.Type.PHOTO && newResponse.isNotNullOrEmpty()
      }
      // TODO: Instead of using toString(), add a method getSerializedValue() in TaskData.
      .map { (_, _, newResponse): TaskDataDelta -> newResponse.toString() }
      .forEach { remotePath: String -> photoSyncWorkManager.enqueueSyncWorker(remotePath) }

  private fun groupByUserId(pendingMutations: List<Mutation>): Map<String, List<Mutation>> =
    pendingMutations.groupBy { it.userId }

  private fun incrementRetryCounts(mutations: List<Mutation>, error: Throwable): List<Mutation> =
    mutations.map { m: Mutation -> incrementRetryCount(m, error) }

  private fun incrementRetryCount(mutation: Mutation, error: Throwable): Mutation =
    when (mutation) {
      is LocationOfInterestMutation ->
        mutation.copy(retryCount = mutation.retryCount + 1, lastError = error.toString())
      is SubmissionMutation ->
        mutation.copy(retryCount = mutation.retryCount + 1, lastError = error.toString())
    }

  companion object {
    private const val LOCATION_OF_INTEREST_ID_PARAM_KEY = "locationOfInterestId"

    /** Returns a new work [Data] object containing the specified location of interest id. */
    @JvmStatic
    fun createInputData(locationOfInterestId: String): Data =
      Data.Builder().putString(LOCATION_OF_INTEREST_ID_PARAM_KEY, locationOfInterestId).build()
  }
}
