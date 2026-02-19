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
package org.groundplatform.android.data.local.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration_125_126 =
  object : Migration(125, 126) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "DELETE FROM survey " +
          "WHERE id IN (" +
          "SELECT DISTINCT j.survey_id " +
          "FROM job j " +
          "JOIN task t ON j.id = t.job_id " +
          "JOIN condition c ON t.id = c.parent_task_id " +
          "LEFT JOIN expression e ON c.parent_task_id = e.parent_task_id " +
          "WHERE e.parent_task_id IS NULL" +
          ")"
      )
    }
  }
