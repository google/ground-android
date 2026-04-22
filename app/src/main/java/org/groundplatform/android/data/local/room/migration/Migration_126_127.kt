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

/**
 * LOIs never carry media, so MEDIA_UPLOAD_PENDING (5) is not a valid state for them. It was written
 * by an earlier version of the LocalMutationSyncWorker that tagged every synced mutation as pending
 * media upload. Those rows should be set to COMPLETED (3).
 */
val Migration_126_127 =
  object : Migration(126, 127) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("UPDATE location_of_interest_mutation SET state = 3 WHERE state = 5")
    }
  }
