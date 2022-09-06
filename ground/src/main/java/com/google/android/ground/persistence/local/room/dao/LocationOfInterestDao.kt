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
import com.google.android.ground.persistence.local.room.models.EntityState
import io.reactivex.Flowable
import io.reactivex.Maybe

/** Provides low-level read/write operations of [LocationOfInterestEntity] to/from the local db. */
@Dao
interface LocationOfInterestDao : BaseDao<LocationOfInterestEntity> {
  @Query("SELECT * FROM location_of_interest WHERE survey_id = :surveyId AND state = :state")
  fun findOnceAndStream(
    surveyId: String,
    state: EntityState
  ): Flowable<List<LocationOfInterestEntity>>

  @Query("SELECT * FROM location_of_interest WHERE id = :id")
  fun findById(id: String): Maybe<LocationOfInterestEntity>
}
