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

package com.google.android.ground.repository

import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.model.mutation.Mutation.SyncStatus.*
import com.google.android.ground.model.mutation.Mutation.SyncStatus.FAILED
import com.google.android.ground.model.mutation.Mutation.SyncStatus.IN_PROGRESS
import com.google.android.ground.model.mutation.Mutation.SyncStatus.MEDIA_UPLOAD_PENDING
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.UploadQueueEntry
import com.google.android.ground.persistence.local.stores.LocalLocationOfInterestStore
import com.google.android.ground.persistence.local.stores.LocalSubmissionStore
import com.google.android.ground.system.auth.AuthenticationManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Coordinates persistence of mutations across [LocationOfInterestMutation] and [SubmissionMutation]
 * local data stores.
 */
@Singleton
class MutationRepository
@Inject
constructor(
  private val authenticationManager: AuthenticationManager,
  private val localLocationOfInterestStore: LocalLocationOfInterestStore,
  private val localSubmissionStore: LocalSubmissionStore,
) {
  /**
   * Returns a long-lived stream that emits the full list of mutations for specified survey on
   * subscribe and a new list on each subsequent change.
   */
  fun getSurveyMutationsFlow(survey: Survey): Flow<List<Mutation>> {
    // TODO(https://github.com/google/ground-android/issues/2838): Show mutations for all surveys,
    //   not just current one.
    // TODO(https://github.com/google/ground-android/issues/2838): This method is also named
    //   incorrectly - it only returns one of LOI or submission mutations. We should delete this
    //   method in favor of [getUploadQueueFlow()].
    val locationOfInterestMutations = localLocationOfInterestStore.getAllSurveyMutations(survey)
    val submissionMutations = localSubmissionStore.getAllSurveyMutationsFlow(survey)

    return locationOfInterestMutations.combine(submissionMutations, this::combineAndSortMutations)
  }

  /**
   * Return the set of data upload queue entries not yet marked as completed sorted in chronological
   * order (FIFO). Media/photo uploads are not included.
   */
  suspend fun getIncompleteUploads(): List<UploadQueueEntry> =
    getUploadQueueFlow().first().filter {
      setOf(PENDING, IN_PROGRESS, FAILED, UNKNOWN).contains(it.uploadStatus)
    }

  /**
   * Return the set of photo/media upload queue entries not yet marked as completed, sorted in
   * chronological order (FIFO).
   */
  suspend fun getIncompleteMediaMutations(): List<SubmissionMutation> =
    getUploadQueueFlow()
      .first()
      .filter {
        setOf(MEDIA_UPLOAD_PENDING, MEDIA_UPLOAD_IN_PROGRESS, MEDIA_UPLOAD_AWAITING_RETRY)
          .contains(it.uploadStatus)
      }
      // TODO(https://github.com/google/ground-android/issues/2120):
      //  Return [MediaMutations] instead once introduced.
      .mapNotNull { it.submissionMutation }

  /**
   * Returns a [Flow] which emits the upload queue once and on each change, sorted in chronological
   * order (FIFO).
   */
  private fun getUploadQueueFlow(): Flow<List<UploadQueueEntry>> =
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
    localLocationOfInterestStore.updateAll(loiMutations)

    val submissionMutations = mutations.filterIsInstance<SubmissionMutation>()
    localSubmissionStore.updateAll(submissionMutations)
  }

  /**
   * Mark pending mutations as ready for media upload. If the mutation is of type DELETE, also
   * removes the corresponding submission or LOI.
   */
  suspend fun finalizePendingMutationsForMediaUpload(mutations: List<Mutation>) {
    finalizeDeletions(mutations)
    // TODO(https://github.com/google/ground-android/issues/2873): Only do this is there are
    // actually photos to upload.
    markForMediaUpload(mutations)
  }

  private suspend fun finalizeDeletions(mutations: List<Mutation>) =
    mutations
      .filter { it.type === Mutation.Type.DELETE }
      .map { mutation ->
        when (mutation) {
          is SubmissionMutation -> {
            localSubmissionStore.deleteSubmission(mutation.submissionId)
          }
          is LocationOfInterestMutation -> {
            localLocationOfInterestStore.deleteLocationOfInterest(mutation.locationOfInterestId)
          }
        }
      }

  suspend fun markAsInProgress(mutations: List<Mutation>) {
    saveMutationsLocally(mutations.updateMutationStatus(IN_PROGRESS))
  }

  suspend fun markAsMediaUploadInProgress(mutations: List<SubmissionMutation>) {
    saveMutationsLocally(mutations.updateMutationStatus(MEDIA_UPLOAD_IN_PROGRESS))
  }

  suspend fun markAsComplete(mutations: List<Mutation>) {
    saveMutationsLocally(mutations.updateMutationStatus(COMPLETED))
  }

  suspend fun markAsFailed(mutations: List<Mutation>, error: Throwable) {
    saveMutationsLocally(mutations.updateMutationStatus(FAILED, error))
  }

  private suspend fun markForMediaUpload(mutations: List<Mutation>) {
    saveMutationsLocally(mutations.updateMutationStatus(MEDIA_UPLOAD_PENDING))
  }

  suspend fun markAsFailedMediaUpload(mutations: List<SubmissionMutation>, error: Throwable) {
    saveMutationsLocally(mutations.updateMutationStatus(MEDIA_UPLOAD_AWAITING_RETRY, error))
  }

  private fun combineAndSortMutations(
    locationOfInterestMutations: List<LocationOfInterestMutation>,
    submissionMutations: List<SubmissionMutation>,
  ): List<Mutation> =
    (locationOfInterestMutations + submissionMutations)
      .groupBy { it.collectionId }
      .map { it.value.reduce { a, b -> if (a.clientTimestamp > b.clientTimestamp) a else b } }
      .sortedWith(Mutation.byDescendingClientTimestamp())
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
