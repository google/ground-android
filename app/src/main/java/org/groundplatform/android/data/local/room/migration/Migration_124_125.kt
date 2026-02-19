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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration_124_125 =
  object : Migration(124, 125) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "CREATE TABLE IF NOT EXISTS survey_temp (" +
          "id TEXT NOT NULL, " +
          "title TEXT, " +
          "description TEXT, " +
          "acl TEXT, " +
          "data_sharing_terms BLOB, " +
          "general_access INTEGER NOT NULL DEFAULT 0, " +
          "data_visibility INTEGER, " +
          "PRIMARY KEY(id)" +
          ")"
      )

      db.execSQL(
        "INSERT INTO survey_temp (id, title, description, acl, data_sharing_terms, general_access, data_visibility) " +
          "SELECT id, title, description, acl, data_sharing_terms, COALESCE(general_access, 0), data_visibility " +
          "FROM survey"
      )

      db.execSQL("DROP TABLE survey")
      db.execSQL("ALTER TABLE survey_temp RENAME TO survey")
    }
  }
