/*
 * Copyright 2024 Google LLC
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
import org.groundplatform.android.persistence.local.room.entity.DraftSubmissionEntity

/** Data access object for database operations related to [DraftSubmissionDao]. */
@Dao
interface DraftSubmissionDao : BaseDao<DraftSubmissionEntity> {

  @Query("SELECT * FROM draft_submission WHERE id = :draftSubmissionId")
  suspend fun findById(draftSubmissionId: String): DraftSubmissionEntity?

  @Query("DELETE FROM draft_submission") fun delete()

  @Query("SELECT COUNT(*) FROM draft_submission") suspend fun countAll(): Int
}
