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
import org.groundplatform.android.persistence.local.room.entity.LocationOfInterestMutationEntity
import org.groundplatform.android.persistence.local.room.fields.MutationEntitySyncStatus

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
  ): List<LocationOfInterestMutationEntity>?
}
