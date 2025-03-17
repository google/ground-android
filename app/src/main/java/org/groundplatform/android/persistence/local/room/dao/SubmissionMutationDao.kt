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
package org.groundplatform.android.persistence.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.groundplatform.android.persistence.local.room.entity.SubmissionMutationEntity
import org.groundplatform.android.persistence.local.room.fields.MutationEntitySyncStatus
import org.groundplatform.android.persistence.local.room.fields.MutationEntityType

/** Data access object for database operations related to [SubmissionMutationEntity]. */
@Dao
interface SubmissionMutationDao : BaseDao<SubmissionMutationEntity> {
  @Query("SELECT * FROM submission_mutation")
  fun getAllMutationsFlow(): Flow<List<SubmissionMutationEntity>>

  @Query(
    "SELECT * FROM submission_mutation " +
      "WHERE location_of_interest_id = :locationOfInterestId AND state IN (:allowedStates)"
  )
  suspend fun findByLocationOfInterestId(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus,
  ): List<SubmissionMutationEntity>

  @Query(
    "SELECT COUNT(*) FROM submission_mutation " +
      "WHERE location_of_interest_id = :loiId AND state IN (:allowedStates) " +
      "AND type = :mutationType"
  )
  suspend fun getSubmissionMutationCount(
    loiId: String,
    mutationType: MutationEntityType,
    vararg allowedStates: MutationEntitySyncStatus,
  ): Int

  @Query(
    "SELECT * FROM submission_mutation " +
      "WHERE submission_id = :submissionId AND state IN (:allowedStates)"
  )
  suspend fun findBySubmissionId(
    submissionId: String,
    vararg allowedStates: MutationEntitySyncStatus,
  ): List<SubmissionMutationEntity>?

  @Query(
    "SELECT * FROM submission_mutation " +
      "WHERE location_of_interest_id = :locationOfInterestId AND state IN (:allowedStates)"
  )
  fun findByLoiIdFlow(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus,
  ): Flow<List<SubmissionMutationEntity>>
}
