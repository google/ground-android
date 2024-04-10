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
package com.google.android.ground.persistence.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Persisted representation of an on-device photo that has been associated with the application,
 * usually through data submission. We do not store associations between photos and the submissions,
 * this table is merely a record of the on-device data itself. Uploads and submission history are
 * modelled through the use of media mutations and are processed at submission time.
 */
@Entity(tableName = "photo")
data class PhotoEntity(
  @ColumnInfo(name = "id") val id: String,
  @ColumnInfo(name = "uri") val uri: String,
)
