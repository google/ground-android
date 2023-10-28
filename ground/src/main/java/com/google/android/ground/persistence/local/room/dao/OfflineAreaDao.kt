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
import com.google.android.ground.persistence.local.room.entity.OfflineAreaEntity
import kotlinx.coroutines.flow.Flow

/** Provides read/write operations for writing [OfflineAreaEntity] to the local db. */
@Dao
interface OfflineAreaDao : BaseDao<OfflineAreaEntity> {
  @Query("SELECT * FROM offline_area") fun findAll(): Flow<List<OfflineAreaEntity>>

  @Query("SELECT * FROM offline_area WHERE id = :id")
  suspend fun findById(id: String): OfflineAreaEntity?

  @Query("DELETE FROM offline_area WHERE id = :id") suspend fun deleteById(id: String)
}
