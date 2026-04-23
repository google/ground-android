/*
 * Copyright 2023 Google LLC
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

package org.groundplatform.android.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.groundplatform.android.data.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.data.local.stores.LocalSubmissionStore
import org.groundplatform.android.data.remote.RemoteDataStore
import org.groundplatform.android.system.auth.AuthenticationManager
import org.groundplatform.android.util.priority
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.mutation.LocationOfInterestMutation
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.COMPLETED
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.FAILED
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.IN_PROGRESS
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.MEDIA_UPLOAD_AWAITING_RETRY
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.MEDIA_UPLOAD_IN_PROGRESS
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.MEDIA_UPLOAD_PENDING
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.PENDING
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.UNKNOWN
import org.groundplatform.domain.model.mutation.SubmissionMutation
import org.groundplatform.domain.model.submission.UploadQueueEntry
import org.groundplatform.domain.repository.MutationRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
import timber.log.Timber

@Singleton
class MutationRepository
@Inject
constructor(
  private val authenticationManager: AuthenticationManager,
  private val localLocationOfInterestStore: LocalLocationOfInterestStore,
  private val localSubmissionStore: LocalSubmissionStore,
  private val remoteDataStore: RemoteDataStore,
  private val userRepository: UserRepositoryInterface,
) : MutationRepositoryInterface {
  override suspend fun getIncompleteUploads(): List<UploadQueueEntry> =
    getUploadQueueFlow().first().filter {
      setOf(PENDING, IN_PROGRESS, FAILED, UNKNOWN).contains(it.uploadStatus)
    }

  override suspend fun getIncompleteMediaMutations(): List<SubmissionMutation> =
    getUploadQueueFlow()
      .first()
      .filter {
        setOf(MEDIA_UPLOAD_PENDING, MEDIA_UPLOAD_IN_PROGRESS, MEDIA_UPLOAD_AWAITING_RETRY)
          .contains(it.uploadStatus)
      }
      // TODO: Return [MediaMutations] instead once introduced.
      // Issue URL: https://github.com/google/ground-android/issues/2120
      .mapNotNull { it.submissionMutation }

  override fun getUploadQueueFlow(): Flow<List<UploadQueueEntry>> =
    localLocationOfInterestStore.getAllMutationsFlow().combine(
      localSubmissionStore.getAllMutationsFlow()
    ) { loiMutations, submissionMutations ->
      buildUploadQueue(loiMutations, submissionMutations)
    }

  private suspend fun buildUploadQueue(
    loiMutations: List<LocationOfInterestMutation>,
    submissionMutations: List<SubmissionMutation>,
  ): List<UploadQueueEntry> {
    val user = authenticationManager.getAuthenticatedUser()
    val loiMutationMap = loiMutations.filterByUser(user).associateBy { it.collectionId }
    val submissionMutationMap =
      submissionMutations.filterByUser(user).associateBy { it.collectionId }
    val collectionIds: Set<String> = loiMutationMap.keys + submissionMutationMap.keys
    return collectionIds
      .map {
        val loiMutation = loiMutationMap[it]
        val submissionMutation = submissionMutationMap[it]
        val userId = submissionMutation?.userId ?: loiMutation!!.userId
        val clientTimestamp = submissionMutation?.clientTimestamp ?: loiMutation!!.clientTimestamp
        val syncStatus = submissionMutation?.syncStatus ?: loiMutation!!.syncStatus
        UploadQueueEntry(userId, clientTimestamp, syncStatus, loiMutation, submissionMutation)
      }
      .sortedBy { it.clientTimestamp }
  }

  private fun <T : Mutation> List<T>.filterByUser(user: User): List<T> {
    val (validMutations, invalidMutations) = partition { it.userId == user.id }
    if (invalidMutations.isNotEmpty()) {
      Timber.e("Mutation(s) not deleted on sign-out")
    }
    return validMutations
  }

  /**
   * Saves the provided list of mutations to local storage. Updates any locally stored, existing
   * mutations to reflect the mutations in the list, creating new mutations as needed.
   */
  private suspend fun saveMutationsLocally(mutations: List<Mutation>) {
    val loiMutations = mutations.filterIsInstance<LocationOfInterestMutation>()
    if (loiMutations.isNotEmpty()) localLocationOfInterestStore.updateAll(loiMutations)

    val submissionMutations = mutations.filterIsInstance<SubmissionMutation>()
    if (submissionMutations.isNotEmpty()) localSubmissionStore.updateAll(submissionMutations)
  }

  override suspend fun processMutations(
    mutations: List<Mutation>
  ): MutationRepositoryInterface.MutationResult =
    try {
    markAsInProgress(mutations)
    uploadMutations(mutations)
    finalizeDeletions(mutations)
    val (hasMediaToUpload, hasNoMedia) =
      mutations.partition { it is SubmissionMutation && it.getPhotoData().isNotEmpty() }
    if (hasNoMedia.isNotEmpty()) markAsComplete(hasNoMedia)
    if (hasMediaToUpload.isNotEmpty()) markForMediaUpload(hasMediaToUpload)
    MutationRepositoryInterface.MutationResult.Success(hasMediaToUpload.isNotEmpty())
    } catch (t: Throwable) {
      // Mark all mutations as having failed since the remote datastore only commits when all
      // mutations have succeeded.
      markAsFailed(mutations, t)
      Timber.log(t.priority(), t, "Failed to sync local data")
      MutationRepositoryInterface.MutationResult.Failure
    }

  private suspend fun finalizeDeletions(mutations: List<Mutation>) =
    mutations
      .filter { it.type === Mutation.Type.DELETE }
      .forEach { mutation ->
        when (mutation) {
          is SubmissionMutation -> {
            localSubmissionStore.deleteSubmission(mutation.submissionId)
          }
          is LocationOfInterestMutation -> {
            localLocationOfInterestStore.deleteLocationOfInterest(mutation.locationOfInterestId)
          }
        }
      }

  private suspend fun markAsInProgress(mutations: List<Mutation>) {
    saveMutationsLocally(mutations.updateMutationStatus(IN_PROGRESS))
  }

  private suspend fun uploadMutations(mutations: List<Mutation>) {
    val user = userRepository.getAuthenticatedUser()
    remoteDataStore.applyMutations(mutations, user)
  }

  override suspend fun markAsMediaUploadInProgress(mutations: List<SubmissionMutation>) {
    saveMutationsLocally(mutations.updateMutationStatus(MEDIA_UPLOAD_IN_PROGRESS))
  }

  override suspend fun markAsComplete(mutations: List<Mutation>) {
    saveMutationsLocally(mutations.updateMutationStatus(COMPLETED))
  }

  override suspend fun markAsFailed(mutations: List<Mutation>, error: Throwable) {
    saveMutationsLocally(mutations.updateMutationStatus(FAILED, error))
  }

  private suspend fun markForMediaUpload(mutations: List<Mutation>) {
    saveMutationsLocally(mutations.updateMutationStatus(MEDIA_UPLOAD_PENDING))
  }

  override suspend fun markAsFailedMediaUpload(
    mutations: List<SubmissionMutation>,
    error: Throwable,
  ) {
    saveMutationsLocally(mutations.updateMutationStatus(MEDIA_UPLOAD_AWAITING_RETRY, error))
  }
}

private fun List<Mutation>.updateMutationStatus(
  syncStatus: SyncStatus,
  error: Throwable? = null,
): List<Mutation> = map {
  val hasSyncFailed = syncStatus == FAILED || syncStatus == MEDIA_UPLOAD_AWAITING_RETRY
  val retryCount = if (hasSyncFailed) it.retryCount + 1 else it.retryCount
  val errorMessage = if (hasSyncFailed) error?.message ?: error.toString() else it.lastError

  when (it) {
    is LocationOfInterestMutation ->
      it.copy(syncStatus = syncStatus, retryCount = retryCount, lastError = errorMessage)
    is SubmissionMutation ->
      it.copy(syncStatus = syncStatus, retryCount = retryCount, lastError = errorMessage)
  }
}
