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
package org.groundplatform.android.data.local.stores

import kotlinx.coroutines.flow.Flow
import org.groundplatform.android.data.local.room.entity.SubmissionMutationEntity
import org.groundplatform.android.data.local.room.fields.MutationEntitySyncStatus
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.mutation.SubmissionMutation
import org.groundplatform.android.model.submission.DraftSubmission
import org.groundplatform.android.model.submission.Submission

interface LocalSubmissionStore : LocalMutationStore<SubmissionMutation, Submission> {
  /**
   * Returns the list of submissions which are not marked for deletion for the specified
   * locationOfInterest and job.
   */
  suspend fun getSubmissions(
    locationOfInterest: LocationOfInterest,
    jobId: String,
  ): List<Submission>

  /** Returns the submission with the specified UUID from the local data store, if found. */
  suspend fun getSubmission(
    locationOfInterest: LocationOfInterest,
    submissionId: String,
  ): Submission

  /** Deletes submission from local database. */
  suspend fun deleteSubmission(submissionId: String)

  /**
   * Returns a [Flow] that emits the list of [SubmissionMutation] instances for a given LOI which
   * match the provided `allowedStates`. A new list is emitted on each subsequent change.
   */
  fun getSubmissionMutationsByLoiIdFlow(
    survey: Survey,
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus,
  ): Flow<List<SubmissionMutation>>

  /**
   * Returns a [Flow] that emits a list of all [SubmissionMutation]s associated with a given
   * [Survey]. The list is newly emitted each time the underlying local data changes.
   */
  fun getAllSurveyMutationsFlow(survey: Survey): Flow<List<SubmissionMutation>>

  fun getAllMutationsFlow(): Flow<List<SubmissionMutation>>

  suspend fun findByLocationOfInterestId(
    loidId: String,
    vararg states: MutationEntitySyncStatus,
  ): List<SubmissionMutationEntity>

  suspend fun getPendingCreateCount(loiId: String): Int

  suspend fun getPendingDeleteCount(loiId: String): Int

  /** Fetches the draft submission for the given UUID from local database. */
  suspend fun getDraftSubmission(draftSubmissionId: String, survey: Survey): DraftSubmission?

  /** Saves the given draft submission to local database. */
  suspend fun saveDraftSubmission(draftSubmission: DraftSubmission)

  /** Removes all locally stored draft submissions. */
  suspend fun deleteDraftSubmissions()

  /** Counts the number of draft submissions in the local database. */
  suspend fun countDraftSubmissions(): Int
}
