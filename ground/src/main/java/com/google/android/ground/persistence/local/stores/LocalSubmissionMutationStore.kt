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
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.rx.annotations.Cold
import com.google.common.collect.ImmutableList
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

interface LocalSubmissionMutationStore : LocalMutationStore<SubmissionMutation, Submission> {
  /**
   * Returns the list of submissions which are not marked for deletion for the specified
   * locationOfInterest and job.
   */
  fun getSubmissions(
    locationOfInterest: LocationOfInterest,
    jobId: String
  ): @Cold Single<ImmutableList<Submission>>

  /** Returns the submission with the specified UUID from the local data store, if found. */
  fun getSubmission(
    locationOfInterest: LocationOfInterest,
    submissionId: String
  ): @Cold Maybe<Submission>

  /** Deletes submission from local database. */
  fun deleteSubmission(submissionId: String): @Cold Completable

  /**
   * Emits the list of [SubmissionMutation] instances for a given LOI which match the provided
   * `allowedStates`. A new list is emitted on each subsequent change.
   */
  fun getSubmissionMutationsByLocationOfInterestIdOnceAndStream(
    survey: Survey,
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Flowable<ImmutableList<SubmissionMutation>>

  fun getAllMutationsAndStream(): Flowable<List<SubmissionMutationEntity>>

  fun findByLocationOfInterestId(
    id: String,
    vararg states: MutationEntitySyncStatus
  ): Single<List<SubmissionMutationEntity>>
}
