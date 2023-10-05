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
package com.google.android.ground.persistence.local.stores

import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.persistence.local.room.entity.SubmissionMutationEntity
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import io.reactivex.Flowable

interface LocalSubmissionStore : LocalMutationStore<SubmissionMutation, Submission> {
  /**
   * Returns the list of submissions which are not marked for deletion for the specified
   * locationOfInterest and job.
   */
  suspend fun getSubmissions(
    locationOfInterest: LocationOfInterest,
    jobId: String
  ): List<Submission>

  /** Returns the submission with the specified UUID from the local data store, if found. */
  suspend fun getSubmission(
    locationOfInterest: LocationOfInterest,
    submissionId: String
  ): Submission

  /** Deletes submission from local database. */
  suspend fun deleteSubmission(submissionId: String)

  /**
   * Emits the list of [SubmissionMutation] instances for a given LOI which match the provided
   * `allowedStates`. A new list is emitted on each subsequent change.
   */
  fun getSubmissionMutationsByLocationOfInterestIdOnceAndStream(
    survey: Survey,
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Flowable<List<SubmissionMutation>>

  fun getAllMutationsAndStream(): Flowable<List<SubmissionMutationEntity>>

  suspend fun findByLocationOfInterestId(
    loidId: String,
    vararg states: MutationEntitySyncStatus
  ): List<SubmissionMutationEntity>

  suspend fun getPendingCreateCount(
    loiId: String,
  ): Int

  suspend fun getPendingDeleteCount(
    loiId: String,
  ): Int
}
