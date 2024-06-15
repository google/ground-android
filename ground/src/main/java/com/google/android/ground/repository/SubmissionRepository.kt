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

import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.DraftSubmission
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.stores.LocalSubmissionStore
import com.google.android.ground.persistence.sync.MutationSyncWorkManager
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.proto.AuditInfo
import com.google.android.ground.proto.Submission
import com.google.protobuf.Timestamp
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

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

  suspend fun createSubmission(surveyId: String, locationOfInterestId: String): Submission {
    val user = userRepository.getAuthenticatedUser()
    val auditInfo =
      AuditInfo.newBuilder()
        .setDisplayName(user.displayName)
        .setPhotoUrl(user.photoUrl)
        .setClientTimestamp(Timestamp.newBuilder().setSeconds(Date().time))
        .build()

    val loi = locationOfInterestRepository.getOfflineLoi(surveyId, locationOfInterestId)

    return Submission.newBuilder()
      .setId(uuidGenerator.generateUuid())
      .setLoiId(locationOfInterestId)
      .setJobId(loi.job.id)
      .setCreated(auditInfo)
      .build()
  }

  private suspend fun createOrUpdateSubmission(
    submission: com.google.android.ground.model.submission.Submission,
    deltas: List<ValueDelta>,
    isNew: Boolean,
  ) =
    applyAndEnqueue(
      SubmissionMutation(
        job = submission.job,
        submissionId = submission.id,
        deltas = deltas,
        type = if (isNew) Mutation.Type.CREATE else Mutation.Type.UPDATE,
        syncStatus = SyncStatus.PENDING,
        surveyId = submission.surveyId,
        locationOfInterestId = submission.locationOfInterest.id,
        userId = submission.lastModified.user.id,
      )
    )

  suspend fun saveSubmission(
    surveyId: String,
    locationOfInterestId: String,
    deltas: List<ValueDelta>,
  ) {
    val submission = createSubmission(surveyId, locationOfInterestId)
    createOrUpdateSubmission(submission, deltas, isNew = true)
  }

  suspend fun getDraftSubmission(draftSubmissionId: String, survey: Survey): DraftSubmission? =
    localSubmissionStore.getDraftSubmission(draftSubmissionId = draftSubmissionId, survey = survey)

  suspend fun saveDraftSubmission(
    jobId: String,
    loiId: String?,
    surveyId: String,
    deltas: List<ValueDelta>,
    loiName: String?,
  ) {
    val newId = uuidGenerator.generateUuid()
    val draft = DraftSubmission(newId, jobId, loiId, loiName, surveyId, deltas)
    localSubmissionStore.saveDraftSubmission(draftSubmission = draft)
    localValueStore.draftSubmissionId = newId
  }

  suspend fun deleteDraftSubmission() {
    localSubmissionStore.deleteDraftSubmissions()
    localValueStore.draftSubmissionId = null
  }

  private suspend fun applyAndEnqueue(mutation: SubmissionMutation) {
    localSubmissionStore.applyAndEnqueue(mutation)
    mutationSyncWorkManager.enqueueSyncWorker(mutation.locationOfInterestId)
  }

  suspend fun getTotalSubmissionCount(loi: LocationOfInterest) =
    loi.submissionCount + getPendingCreateCount(loi.id) - getPendingDeleteCount(loi.id)

  private suspend fun getPendingCreateCount(loiId: String) =
    localSubmissionStore.getPendingCreateCount(loiId)

  private suspend fun getPendingDeleteCount(loiId: String) =
    localSubmissionStore.getPendingDeleteCount(loiId)
}
