/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.data.local.room.migration

import android.content.ContentValues
import org.groundplatform.android.proto.Survey

object MigrationTestDataGenerator {
  fun getSurveyContentValues(
    id: String = "1",
    title: String = "title",
    description: String = "description",
    generalAccess: Int = Survey.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED.ordinal,
  ): ContentValues =
    ContentValues().apply {
      put("id", id)
      put("title", title)
      put("description", description)
      put("general_access", generalAccess)
    }

  fun getJobContentValues(id: String = "1", surveyId: String = "1"): ContentValues =
    ContentValues().apply {
      put("id", id)
      put("survey_id", surveyId)
      put("name", "Job $id")
      put("strategy", "ad-hoc")
    }

  fun getTaskContentValues(id: String = "1", jobId: String = "1"): ContentValues =
    ContentValues().apply {
      put("id", id)
      put("job_id", jobId)
      put("\"index\"", 0)
      put("task_type", 1)
      put("is_required", false)
      put("is_add_loi_task", false)
    }

  fun getConditionContentValues(parentTaskId: String = "1"): ContentValues =
    ContentValues().apply {
      put("parent_task_id", parentTaskId)
      put("match_type", 1)
    }

  fun getExpressionContentValues(parentTaskId: String = "1"): ContentValues =
    ContentValues().apply {
      put("parent_task_id", parentTaskId)
      put("task_id", "someOtherTask")
      put("expression_type", 1)
    }
}
