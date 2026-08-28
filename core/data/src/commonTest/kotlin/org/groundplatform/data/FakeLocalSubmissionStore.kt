/*
 * Copyright 2026 Google LLC
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

package org.groundplatform.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.groundplatform.data.stores.LocalSubmissionStore
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus
import org.groundplatform.domain.model.mutation.SubmissionMutation
import org.groundplatform.domain.model.submission.DraftSubmission
import org.groundplatform.domain.model.submission.Submission

class FakeLocalSubmissionStore : LocalSubmissionStore {
  val appliedMutations = mutableListOf<SubmissionMutation>()
  val draftSubmissions = mutableMapOf<String, DraftSubmission>()
  var pendingCreateCount: Int = 0
  var pendingDeleteCount: Int = 0
  var submissionsToReturn: List<Submission> = emptyList()

  override suspend fun getSubmissions(
    locationOfInterest: LocationOfInterest,
    jobId: String,
  ): List<Submission> = submissionsToReturn

  override suspend fun getSubmission(
    locationOfInterest: LocationOfInterest,
    submissionId: String,
  ): Submission = submissionsToReturn.first { it.id == submissionId }

  override suspend fun deleteSubmission(submissionId: String) {
    submissionsToReturn = submissionsToReturn.filterNot { it.id == submissionId }
  }

  override fun getSubmissionMutationsByLoiIdFlow(
    survey: Survey,
    locationOfInterestId: String,
    vararg allowedStates: SyncStatus,
  ): Flow<List<SubmissionMutation>> = emptyFlow()

  override fun getAllSurveyMutationsFlow(survey: Survey): Flow<List<SubmissionMutation>> =
    emptyFlow()

  override fun getAllMutationsFlow(): Flow<List<SubmissionMutation>> = emptyFlow()

  override suspend fun findByLocationOfInterestId(
    loidId: String,
    vararg states: SyncStatus,
  ): List<SubmissionMutation> = emptyList()

  override suspend fun getPendingCreateCount(loiId: String): Int = pendingCreateCount

  override suspend fun getPendingDeleteCount(loiId: String): Int = pendingDeleteCount

  override suspend fun getDraftSubmission(
    draftSubmissionId: String,
    survey: Survey,
  ): DraftSubmission? = draftSubmissions[draftSubmissionId]?.takeIf { it.surveyId == survey.id }

  override suspend fun saveDraftSubmission(draftSubmission: DraftSubmission) {
    draftSubmissions[draftSubmission.id] = draftSubmission
  }

  override suspend fun deleteDraftSubmissions() {
    draftSubmissions.clear()
  }

  override suspend fun countDraftSubmissions(): Int = draftSubmissions.size

  override suspend fun merge(model: Submission) = Unit

  override suspend fun enqueue(mutation: SubmissionMutation) {
    appliedMutations.add(mutation)
  }

  override suspend fun apply(mutation: SubmissionMutation) {
    appliedMutations.add(mutation)
  }

  override suspend fun updateAll(mutations: List<SubmissionMutation>) {
    appliedMutations.addAll(mutations)
  }

  override suspend fun applyAndEnqueue(mutation: SubmissionMutation) {
    appliedMutations.add(mutation)
  }
}
