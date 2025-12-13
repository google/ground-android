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

/**
 * Clean up the "task" table to force a refetch of tasks from the remote server. This fixes an issue
 * where conditional tasks were inserted out of order, leading to broken foreign key constraints.
 */
val Migration_125_126 =
  object : Migration(125, 126) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("DELETE FROM task")
    }
  }
