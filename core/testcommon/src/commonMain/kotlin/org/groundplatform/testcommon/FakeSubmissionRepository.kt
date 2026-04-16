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
package org.groundplatform.testcommon

import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.submission.DraftSubmission
import org.groundplatform.domain.model.submission.ValueDelta
import org.groundplatform.domain.repository.SubmissionRepositoryInterface

class FakeSubmissionRepository : SubmissionRepositoryInterface {
  var draftSubmission: List<DraftSubmission> = emptyList()
  var latestDraftSubmissionId: String = ""
  var pendingCreateCount: Int = 0
  var pendingDeleteCount: Int = 0
  var onSaveSubmissionCall = FakeCall<SaveSubmissionParams, Unit> {}

  override suspend fun saveSubmission(
    surveyId: String,
    locationOfInterestId: String,
    deltas: List<ValueDelta>,
    collectionId: String,
  ) {
    onSaveSubmissionCall(SaveSubmissionParams(surveyId, locationOfInterestId, deltas, collectionId))
  }

  override suspend fun getDraftSubmission(
    draftSubmissionId: String,
    survey: Survey,
  ): DraftSubmission? = draftSubmission.firstOrNull { it.id == draftSubmissionId }

  override suspend fun countDraftSubmissions(): Int = draftSubmission.count()

  override fun getDraftSubmissionsId(): String = latestDraftSubmissionId

  override suspend fun saveDraftSubmission(
    jobId: String,
    loiId: String?,
    surveyId: String,
    deltas: List<ValueDelta>,
    loiName: String?,
    currentTaskId: String,
  ) {
    draftSubmission +=
      FakeDataGenerator.newDraftSubmission(
        jobId = jobId,
        loiId = loiId,
        surveyId = surveyId,
        deltas = deltas,
        loiName = loiName,
        currentTaskId = currentTaskId,
      )
  }

  override suspend fun deleteDraftSubmission() {
    draftSubmission = emptyList()
    latestDraftSubmissionId = ""
  }

  override suspend fun getTotalSubmissionCount(loi: LocationOfInterest): Int =
    loi.submissionCount + getPendingCreateCount(loi.id) - pendingDeleteCount

  override suspend fun getPendingCreateCount(loiId: String): Int = pendingCreateCount

  data class SaveSubmissionParams(
    val surveyId: String,
    val loiId: String,
    val deltas: List<ValueDelta>,
    val collectionId: String,
  )
}
