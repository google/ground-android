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
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestEntity
import com.google.android.ground.persistence.local.room.fields.EntityDeletionState
import kotlinx.coroutines.flow.Flow

/** Provides low-level read/write operations of [LocationOfInterestEntity] to/from the local db. */
@Dao
interface LocationOfInterestDao : BaseDao<LocationOfInterestEntity> {

  @Query(
    "SELECT * FROM location_of_interest WHERE survey_id = :surveyId AND state = :deletionState"
  )
  fun getByDeletionState(
    surveyId: String,
    deletionState: EntityDeletionState,
  ): Flow<List<LocationOfInterestEntity>>

  @Query("SELECT * FROM location_of_interest WHERE id = :id")
  suspend fun findById(id: String): LocationOfInterestEntity?

  /**
   * Deletes all LOIs in specified survey whose IDs are not present in the specified list..
   * Main-safe.
   */
  @Query("DELETE FROM location_of_interest WHERE survey_id = :surveyId AND id NOT IN (:ids)")
  suspend fun deleteNotIn(surveyId: String, ids: List<String>)
}
