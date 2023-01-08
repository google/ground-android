/*
 * Copyright 2019 Google LLC
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
package com.google.android.ground.persistence.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.google.android.ground.persistence.local.room.entity.SubmissionMutationEntity
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.rx.annotations.Cold
import io.reactivex.Flowable
import io.reactivex.Single

/** Data access object for database operations related to [SubmissionMutationEntity]. */
@Dao
interface SubmissionMutationDao : BaseDao<SubmissionMutationEntity> {
  @Query("SELECT * FROM submission_mutation")
  fun loadAllOnceAndStream(): Flowable<List<SubmissionMutationEntity>>

  @Query(
    "SELECT * FROM submission_mutation " +
      "WHERE location_of_interest_id = :locationOfInterestId AND state IN (:allowedStates)"
  )
  fun findByLocationOfInterestId(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Single<List<SubmissionMutationEntity>>

  @Query(
    "SELECT * FROM submission_mutation " +
      "WHERE submission_id = :submissionId AND state IN (:allowedStates)"
  )
  fun findBySubmissionId(
    submissionId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Single<List<SubmissionMutationEntity>>

  @Query(
    "SELECT * FROM submission_mutation " +
      "WHERE location_of_interest_id = :locationOfInterestId AND state IN (:allowedStates)"
  )
  fun findByLocationOfInterestIdOnceAndStream(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): @Cold(terminates = false) Flowable<List<SubmissionMutationEntity>>
}
