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
package com.google.android.ground.persistence.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.persistence.local.room.fields.OfflineAreaEntityState

/** Represents a [OfflineArea] in the local data store. */
@Entity(tableName = "offline_area")
data class OfflineAreaEntity(
  @ColumnInfo(name = "id") @PrimaryKey val id: String,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "state") val state: OfflineAreaEntityState,
  @ColumnInfo(name = "north") val north: Double,
  @ColumnInfo(name = "south") val south: Double,
  @ColumnInfo(name = "east") val east: Double,
  @ColumnInfo(name = "west") val west: Double,
  @ColumnInfo(name = "min_zoom") val minZoom: Int,
  @ColumnInfo(name = "max_zoom") val maxZoom: Int
)
