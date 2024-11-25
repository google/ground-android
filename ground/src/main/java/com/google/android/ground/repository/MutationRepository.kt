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
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.model.mutation.Mutation.SyncStatus.*
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.UploadQueueEntry
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestMutationEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionMutationEntity
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.stores.LocalLocationOfInterestStore
import com.google.android.ground.persistence.local.stores.LocalSubmissionStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Coordinates persistence of mutations across [LocationOfInterestMutation] and [SubmissionMutation]
 * local data stores.
 */
@Singleton
class MutationRepository
@Inject
constructor(
  private val localSurveyStore: LocalSurveyStore,
  private val localLocationOfInterestStore: LocalLocationOfInterestStore,
  private val localSubmissionStore: LocalSubmissionStore,
) {
  /**
   * Returns a long-lived stream that emits the full list of mutations for specified survey on
   * subscribe and a new list on each subsequent change.
   */
  fun getSurveyMutationsFlow(survey: Survey): Flow<List<Mutation>> {
    // TODO(https://github.com/google/ground-android/issues/2838): Show mutations for all surveys,
    // not just current one.
    val locationOfInterestMutations = localLocationOfInterestStore.getAllSurveyMutations(survey)
    val submissionMutations = localSubmissionStore.getAllSurveyMutationsFlow(survey)

    return locationOfInterestMutations.combine(submissionMutations, this::combineAndSortMutations)
  }

  fun getAllMutationsFlow(): Flow<List<Mutation>> {
    val locationOfInterestMutations = localLocationOfInterestStore.getAllMutationsFlow()
    val submissionMutations = localSubmissionStore.getAllMutationsFlow()

    return locationOfInterestMutations.combine(submissionMutations, this::combineAndSortMutations)
  }

  /**
   * Returns all local submission mutations associated with the the given LOI ID that have one of
   * the provided sync statues.
   */
  suspend fun getSubmissionMutations(
    loiId: String,
    vararg entitySyncStatus: MutationEntitySyncStatus,
  ) = getMutations(loiId, *entitySyncStatus).filterIsInstance<SubmissionMutation>()

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
  suspend fun getIncompleteMediaUploads(): List<SubmissionMutation> =
    localSubmissionStore
      .getAllMutationsFlow()
      .first()
      .filter {
        setOf(MEDIA_UPLOAD_PENDING, MEDIA_UPLOAD_IN_PROGRESS, FAILED, UNKNOWN)
          .contains(it.syncStatus)
      }
      .sortedBy { it.clientTimestamp }

  /**
   * Returns a [Flow] which emits the upload queue once and on each change, sorted in chronological
   * order (FIFO).
   */
  fun getUploadQueueFlow(): Flow<List<UploadQueueEntry>> {
    return localLocationOfInterestStore.getAllMutationsFlow().combine(
      localSubmissionStore.getAllMutationsFlow()
    ) { loiMutations, submissionMutations ->
      buildUploadQueue(loiMutations, submissionMutations)
    }
  }

  private fun buildUploadQueue(
    loiMutations: List<LocationOfInterestMutation>,
    submissionMutations: List<SubmissionMutation>,
  ): List<UploadQueueEntry> {
    val loiMutationMap = loiMutations.associateBy { it.collectionId }
    val submissionMutationMap = submissionMutations.associateBy { it.collectionId }
    val collectionIds = loiMutationMap.keys + submissionMutationMap.keys
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

  /**
   * Returns all LOI and submission mutations in the local mutation queue relating to LOI with the
   * specified id, sorted by creation timestamp (oldest first).
   */
  suspend fun getMutations(
    loiId: String,
    vararg entitySyncStatus: MutationEntitySyncStatus,
  ): List<Mutation> {
    val loiMutations =
      localLocationOfInterestStore
        .findByLocationOfInterestId(loiId, *entitySyncStatus)
        .map(LocationOfInterestMutationEntity::toModelObject)
    val submissionMutations =
      localSubmissionStore.findByLocationOfInterestId(loiId, *entitySyncStatus).map {
        it.toSubmissionMutation()
      }
    return (loiMutations + submissionMutations).sortedBy { it.clientTimestamp }
  }

  private suspend fun SubmissionMutationEntity.toSubmissionMutation(): SubmissionMutation =
    toModelObject(
      localSurveyStore.getSurveyById(surveyId)
        ?: error("Survey missing $surveyId. Unable to fetch pending submission mutations.")
    )

  /**
   * Saves the provided list of mutations to local storage. Updates any locally stored, existing
   * mutations to reflect the mutations in the list, creating new mutations as needed.
   */
  suspend fun saveMutationsLocally(mutations: List<Mutation>) {
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

  suspend fun markAsFailed(mutations: List<Mutation>, error: Throwable) {
    saveMutationsLocally(mutations.updateMutationStatus(FAILED, error))
  }

  private suspend fun markForMediaUpload(mutations: List<Mutation>) {
    saveMutationsLocally(mutations.updateMutationStatus(MEDIA_UPLOAD_PENDING))
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

// TODO(#2119): Refactor this and the related markAs* methods out of this repository. Workers will
// generally
// want to have control over when work should be retried. This means they may need finer grained
// control over when a mutation is marked as failed and when it is considered eligible for retry
// based on various conditions. Batch marking sequences of mutations prevents this. Instead, let's
// have
// workers operate directly on values List<Mutation> updating them appropriately, then batch write
// these via the repository using saveMutationsLocally.
//
// For example, a worker would do:
//   repo.getMutations(....)
//       .map { doRemoteOrBackgroundWork(it) }
//       .map { if (condition...) it.updateStatus(RETRY) else it.updateStatus(FAILED) } // for
// illustration; we'd likely just do this in "doRemoteOr..."
//       .also { repo.saveMutationsLocally(it) } // write updated mutations to local storage to
// exclude/include them in further processing runs.
private fun List<Mutation>.updateMutationStatus(
  syncStatus: SyncStatus,
  error: Throwable? = null,
): List<Mutation> = map {
  val hasSyncFailed = syncStatus == SyncStatus.FAILED
  val retryCount = if (hasSyncFailed) it.retryCount + 1 else it.retryCount
  val errorMessage = if (hasSyncFailed) error?.message ?: error.toString() else it.lastError

  when (it) {
    is LocationOfInterestMutation ->
      it.copy(syncStatus = syncStatus, retryCount = retryCount, lastError = errorMessage)
    is SubmissionMutation ->
      it.copy(syncStatus = syncStatus, retryCount = retryCount, lastError = errorMessage)
  }
}
