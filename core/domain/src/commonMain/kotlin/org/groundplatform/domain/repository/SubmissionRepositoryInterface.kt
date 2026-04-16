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
package org.groundplatform.domain.repository

import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.submission.DraftSubmission
import org.groundplatform.domain.model.submission.Submission
import org.groundplatform.domain.model.submission.ValueDelta

/**
 * Coordinates persistence and retrieval of [Submission] instances from remote, local, and in memory
 * data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
interface SubmissionRepositoryInterface {
  /** Creates a new submission in the local data store and enqueues a sync worker. */
  suspend fun saveSubmission(
    surveyId: String,
    locationOfInterestId: String,
    deltas: List<ValueDelta>,
    collectionId: String,
  )

  suspend fun getDraftSubmission(draftSubmissionId: String, survey: Survey): DraftSubmission?

  suspend fun countDraftSubmissions(): Int

  fun getDraftSubmissionsId(): String

  suspend fun saveDraftSubmission(
    jobId: String,
    loiId: String?,
    surveyId: String,
    deltas: List<ValueDelta>,
    loiName: String?,
    currentTaskId: String,
  )

  suspend fun deleteDraftSubmission()

  suspend fun getTotalSubmissionCount(loi: LocationOfInterest): Int

  suspend fun getPendingCreateCount(loiId: String): Int
}
