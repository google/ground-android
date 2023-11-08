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
import com.google.android.ground.persistence.local.room.entity.PhotoEntity

/** Data access object for database operations related to [PhotoEntity]. */
@Dao
interface PhotoDao : BaseDao<PhotoEntity> {
  @Query("SELECT * FROM photo WHERE id = :id") suspend fun get(id: String): PhotoEntity

  /** Retrieves all photos stored in the local database. */
  @Query("SELECT * FROM photo") suspend fun getAllPhotos(): List<PhotoEntity>

  /**
   * Retrieves all locally stored photos associated with a given
   * [com.google.android.ground.model.job.Job].
   */
  @Query("SELECT * FROM photo WHERE job_id = :jobId")
  suspend fun getPhotosByJob(jobId: String): List<PhotoEntity>
}
