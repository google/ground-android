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
package com.google.android.ground.persistence.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.google.android.ground.persistence.local.room.entity.PhotoMutationEntity
import kotlinx.coroutines.flow.Flow

/** Data access object for database operations related to [PhotoMutationEntity]. */
@Dao
interface PhotoMutationDao : BaseDao<PhotoMutationEntity> {
  /** Retrieves all photo mutations stored in the local db. */
  @Query("SELECT * FROM photo_mutation") fun getAllMutationsFlow(): Flow<List<PhotoMutationEntity>>

  /** Retrieves all photo mutations associated with a given submission mutation. */
  @Query("SELECT * FROM photo_mutation WHERE submission_mutation_id = :submissionMutationId")
  suspend fun findBySubmissionMutationId(submissionMutationId: String): List<PhotoMutationEntity>

  /**
   * Returns a [Flow] that emits all photo mutations associated with a given submission mutation
   * every time the underlying data in storage changes.
   */
  @Query("SELECT * FROM photo_mutation WHERE submission_mutation_id = :submissionMutationId")
  fun findBySubmissionMutationIdFlow(submissionMutationId: String): Flow<List<PhotoMutationEntity>>
}
