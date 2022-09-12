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
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestMutationEntity
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.rx.annotations.Cold
import io.reactivex.Flowable
import io.reactivex.Single

/**
 * Provides low-level read/write operations of [LocationOfInterestMutationEntity] to/from the local
 * db.
 */
@Dao
interface LocationOfInterestMutationDao : BaseDao<LocationOfInterestMutationEntity> {
  @Query("SELECT * FROM location_of_interest_mutation")
  fun loadAllOnceAndStream(): Flowable<List<LocationOfInterestMutationEntity>>

  @Query(
    "SELECT * FROM location_of_interest_mutation " +
      "WHERE location_of_interest_id = :locationOfInterestId " +
      "AND state IN (:allowedStates)"
  )
  fun findByLocationOfInterestId(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Single<List<LocationOfInterestMutationEntity>>

  @Query(
    "SELECT * FROM location_of_interest_mutation " +
      "WHERE location_of_interest_id = :locationOfInterestId " +
      "AND state IN (:allowedStates)"
  )
  fun findByLocationOfInterestIdOnceAndStream(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): @Cold(terminates = false) Flowable<List<LocationOfInterestMutationEntity>>
}
