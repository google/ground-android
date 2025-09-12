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
package org.groundplatform.android.repository

import javax.inject.Inject
import javax.inject.Singleton
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.local.stores.LocalSubmissionStore
import org.groundplatform.android.data.sync.MutationSyncWorkManager
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.model.mutation.Mutation.SyncStatus
import org.groundplatform.android.model.mutation.SubmissionMutation
import org.groundplatform.android.model.submission.DraftSubmission
import org.groundplatform.android.model.submission.Submission
import org.groundplatform.android.model.submission.ValueDelta

/**
 * Coordinates persistence and retrieval of [Submission] instances from remote, local, and in memory
 * data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
class SubmissionRepository
@Inject
constructor(
  private val localSubmissionStore: LocalSubmissionStore,
  private val localValueStore: LocalValueStore,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val mutationSyncWorkManager: MutationSyncWorkManager,
  private val userRepository: UserRepository,
  private val uuidGenerator: OfflineUuidGenerator,
) {

  /** Creates a new submission in the local data store and enqueues a sync worker. */
  suspend fun saveSubmission(
    surveyId: String,
    locationOfInterestId: String,
    deltas: List<ValueDelta>,
    collectionId: String,
  ) {
    val newId = uuidGenerator.generateUuid()
    val userId = userRepository.getAuthenticatedUser().id
    val job = locationOfInterestRepository.getOfflineLoi(surveyId, locationOfInterestId)?.job
    job?.let {
      val mutation =
        SubmissionMutation(
          job = job,
          submissionId = newId,
          deltas = deltas,
          type = Mutation.Type.CREATE,
          syncStatus = SyncStatus.PENDING,
          surveyId = surveyId,
          locationOfInterestId = locationOfInterestId,
          userId = userId,
          collectionId = collectionId,
        )
      applyAndEnqueue(mutation)
    }
  }

  suspend fun getDraftSubmission(draftSubmissionId: String, survey: Survey): DraftSubmission? =
    localSubmissionStore.getDraftSubmission(draftSubmissionId = draftSubmissionId, survey = survey)

  suspend fun countDraftSubmissions() = localSubmissionStore.countDraftSubmissions()

  fun getDraftSubmissionsId() = localValueStore.draftSubmissionId ?: ""

  suspend fun saveDraftSubmission(
    jobId: String,
    loiId: String?,
    surveyId: String,
    deltas: List<ValueDelta>,
    loiName: String?,
    currentTaskId: String,
  ) {
    val newId = uuidGenerator.generateUuid()
    val draft = DraftSubmission(newId, jobId, loiId, loiName, surveyId, deltas, currentTaskId)
    localSubmissionStore.saveDraftSubmission(draftSubmission = draft)
    localValueStore.draftSubmissionId = newId
  }

  suspend fun deleteDraftSubmission() {
    localSubmissionStore.deleteDraftSubmissions()
    localValueStore.draftSubmissionId = null
  }

  private suspend fun applyAndEnqueue(mutation: SubmissionMutation) {
    localSubmissionStore.applyAndEnqueue(mutation)
    mutationSyncWorkManager.enqueueSyncWorker()
  }

  suspend fun getTotalSubmissionCount(loi: LocationOfInterest) =
    loi.submissionCount + getPendingCreateCount(loi.id) - getPendingDeleteCount(loi.id)

  suspend fun getPendingCreateCount(loiId: String) =
    localSubmissionStore.getPendingCreateCount(loiId)

  private suspend fun getPendingDeleteCount(loiId: String) =
    localSubmissionStore.getPendingDeleteCount(loiId)
}
