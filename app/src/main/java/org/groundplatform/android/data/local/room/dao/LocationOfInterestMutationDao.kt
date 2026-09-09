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
package org.groundplatform.android.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.groundplatform.android.data.local.room.entity.LocationOfInterestMutationEntity
import org.groundplatform.android.data.local.room.fields.MutationEntitySyncStatus
import org.groundplatform.android.data.local.room.fields.MutationEntityType

/**
 * Provides low-level read/write operations of [LocationOfInterestMutationEntity] to/from the local
 * db.
 */
@Dao
interface LocationOfInterestMutationDao : BaseDao<LocationOfInterestMutationEntity> {
  @Query("SELECT * FROM location_of_interest_mutation")
  fun getAllMutationsFlow(): Flow<List<LocationOfInterestMutationEntity>>

  @Query(
    "SELECT * FROM location_of_interest_mutation " +
      "WHERE location_of_interest_id = :locationOfInterestId " +
      "AND state IN (:allowedStates)"
  )
  suspend fun getMutations(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus,
  ): List<LocationOfInterestMutationEntity>

  /** Returns how many of the survey's LOIs hold a mutation of another type in one of the states. */
  @Query(
    "SELECT COUNT(DISTINCT location_of_interest_id) FROM location_of_interest_mutation " +
      "WHERE survey_id = :surveyId " +
      "AND type != :excludedType " +
      "AND state IN (:allowedStates)"
  )
  suspend fun countLocationOfInterestIds(
    surveyId: String,
    excludedType: MutationEntityType,
    vararg allowedStates: MutationEntitySyncStatus,
  ): Int
}
