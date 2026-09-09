/*
 * Copyright 2026 Google LLC
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
import org.groundplatform.android.data.local.room.entity.SurveySyncStateEntity

@Dao
interface SurveySyncStateDao : BaseDao<SurveySyncStateEntity> {
  @Query("SELECT * FROM survey_sync_state WHERE survey_id = :surveyId")
  suspend fun get(surveyId: String): SurveySyncStateEntity?

  @Query(
    "UPDATE survey_sync_state SET latest_loi_server_timestamp = :latestLoiServerTimestamp WHERE survey_id = :surveyId"
  )
  suspend fun updateLatestLoiServerTimestamp(surveyId: String, latestLoiServerTimestamp: Long)
}
