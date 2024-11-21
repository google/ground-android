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
import com.google.android.ground.persistence.local.room.entity.SubmissionEntity
import com.google.android.ground.persistence.local.room.fields.EntityDeletionState

@Dao
interface SubmissionDao : BaseDao<SubmissionEntity> {
  /** Returns the submission with the specified UUID, if found. */
  @Query("SELECT * FROM submission WHERE id = :submissionId")
  suspend fun findById(submissionId: String): SubmissionEntity?

  /**
   * Returns the list submissions associated with the specified location of interest, task and
   * state.
   */
  @Query(
    "SELECT * FROM submission " +
      "WHERE location_of_interest_id = :locationOfInterestId " +
      "AND job_id = :jobId AND state = :deletionState"
  )
  suspend fun findByLocationOfInterestId(
    locationOfInterestId: String,
    jobId: String,
    deletionState: EntityDeletionState,
  ): List<SubmissionEntity>?
}
