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
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.android.ground.model.locationofinterest.LoiProperties
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.room.fields.MutationEntityType

/**
 * Defines how Room persists LOI mutations for remote sync in the local db. By default, Room uses
 * the name of object fields and their respective types to determine database column names and
 * types.
 */
@Entity(
  tableName = "location_of_interest_mutation",
  foreignKeys =
    [
      ForeignKey(
        entity = LocationOfInterestEntity::class,
        parentColumns = ["id"],
        childColumns = ["location_of_interest_id"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("location_of_interest_id")],
)
data class LocationOfInterestMutationEntity(
  @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long? = 0,
  @ColumnInfo(name = "survey_id") val surveyId: String,
  @ColumnInfo(name = "type") val type: MutationEntityType,
  @ColumnInfo(name = "state") val syncStatus: MutationEntitySyncStatus,
  @ColumnInfo(name = "retry_count") val retryCount: Long,
  @ColumnInfo(name = "last_error") val lastError: String,
  @ColumnInfo(name = "user_id") val userId: String,
  @ColumnInfo(name = "client_timestamp") val clientTimestamp: Long,
  @ColumnInfo(name = "location_of_interest_id") val locationOfInterestId: String,
  @ColumnInfo(name = "job_id") val jobId: String,
  @ColumnInfo(name = "is_predefined") val isPredefined: Boolean?,
  @ColumnInfo(name = "collection_id") val collectionId: String,
  /** Non-null if the LOI's geometry was updated, null if unchanged. */
  val newGeometry: GeometryWrapper?,
  val newProperties: LoiProperties,
  val newCustomId: String,
)
