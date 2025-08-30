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
import org.groundplatform.android.data.local.room.entity.JobEntity

@Dao
interface JobDao : BaseDao<JobEntity> {
  @Query("DELETE FROM job WHERE survey_id = :surveyId")
  suspend fun deleteBySurveyId(surveyId: String)

  @Query("DELETE FROM job WHERE survey_id = :surveyId AND id NOT IN (:ids)")
  suspend fun deleteNotIn(surveyId: String, ids: List<String>)
}
