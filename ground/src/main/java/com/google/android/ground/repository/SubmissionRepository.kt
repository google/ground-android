/*
 * Copyright 2018 Google LLC
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

import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.stores.LocalSubmissionStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.sync.MutationSyncWorkManager
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.system.auth.AuthenticationManager
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.rx2.rxCompletable
import kotlinx.coroutines.rx2.rxSingle

/**
 * Coordinates persistence and retrieval of [Submission] instances from remote, local, and in memory
 * data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
class SubmissionRepository
@Inject
constructor(
  private val localSurveyStore: LocalSurveyStore,
  private val localSubmissionStore: LocalSubmissionStore,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val mutationSyncWorkManager: MutationSyncWorkManager,
  private val uuidGenerator: OfflineUuidGenerator,
  private val authManager: AuthenticationManager,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

  fun getSubmission(
    surveyId: String,
    locationOfInterestId: String,
    submissionId: String
  ): @Cold Single<Submission> =
    // TODO: Store and retrieve latest edits from cache and/or db.
    locationOfInterestRepository
      .getOfflineLocationOfInterest(surveyId, locationOfInterestId)
      .flatMap { locationOfInterest ->
        rxSingle { localSubmissionStore.getSubmission(locationOfInterest, submissionId) }
      }

  fun createSubmission(surveyId: String, locationOfInterestId: String): @Cold Single<Submission> {
    val auditInfo = AuditInfo(authManager.currentUser)
    return locationOfInterestRepository
      .getOfflineLocationOfInterest(surveyId, locationOfInterestId)
      .map { locationOfInterest: LocationOfInterest ->
        Submission(
          uuidGenerator.generateUuid(),
          surveyId,
          locationOfInterest,
          locationOfInterest.job,
          auditInfo,
          auditInfo
        )
      }
  }

  fun deleteSubmission(submission: Submission): @Cold Completable =
    applyAndEnqueue(
      SubmissionMutation(
        job = submission.job,
        submissionId = submission.id,
        type = Mutation.Type.DELETE,
        syncStatus = SyncStatus.PENDING,
        surveyId = submission.surveyId,
        locationOfInterestId = submission.locationOfInterest.id,
        userId = authManager.currentUser.id
      )
    )

  fun createOrUpdateSubmission(
    submission: Submission,
    deltas: List<ValueDelta>,
    isNew: Boolean
  ): @Cold Completable =
    applyAndEnqueue(
      SubmissionMutation(
        job = submission.job,
        submissionId = submission.id,
        deltas = deltas,
        type = if (isNew) Mutation.Type.CREATE else Mutation.Type.UPDATE,
        syncStatus = SyncStatus.PENDING,
        surveyId = submission.surveyId,
        locationOfInterestId = submission.locationOfInterest.id,
        userId = authManager.currentUser.id
      )
    )

  fun saveSubmission(
    surveyId: String,
    locationOfInterestId: String,
    deltas: List<ValueDelta>
  ): @Cold Completable =
    createSubmission(surveyId, locationOfInterestId).flatMapCompletable {
      createOrUpdateSubmission(it, deltas, isNew = true)
    }

  private fun applyAndEnqueue(mutation: SubmissionMutation) =
    rxCompletable(ioDispatcher) {
      localSubmissionStore.applyAndEnqueue(mutation)
      mutationSyncWorkManager.enqueueSyncWorker(mutation.locationOfInterestId)
    }

  /**
   * Returns all [SubmissionMutation] instances for a given location of interest which have not yet
   * been marked as [SyncStatus.COMPLETED], including pending, in progress, and failed mutations. A
   * new list is emitted on each subsequent change.
   */
  suspend fun getIncompleteSubmissionMutationsOnceAndStream(
    surveyId: String,
    locationOfInterestId: String
  ): Flow<List<SubmissionMutation>> {
    val survey = localSurveyStore.getSurveyByIdSuspend(surveyId) ?: return flowOf()

    return localSubmissionStore.getSubmissionMutationsByLoiIdFlow(
      survey,
      locationOfInterestId,
      MutationEntitySyncStatus.PENDING,
      MutationEntitySyncStatus.IN_PROGRESS,
      MutationEntitySyncStatus.FAILED
    )
  }

  suspend fun getTotalSubmissionCount(loi: LocationOfInterest) =
    loi.submissionCount + getPendingCreateCount(loi.id) - getPendingDeleteCount(loi.id)

  private suspend fun getPendingCreateCount(loiId: String) =
    localSubmissionStore.getPendingCreateCount(loiId)

  private suspend fun getPendingDeleteCount(loiId: String) =
    localSubmissionStore.getPendingDeleteCount(loiId)
}
