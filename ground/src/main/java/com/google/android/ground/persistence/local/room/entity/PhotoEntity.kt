/*
 * Copyright 2023 Google LLC
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
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A locally persisted representation of a user provided photo associated with a job and LOI. */
@Entity(
  tableName = "photo",
  foreignKeys =
    [
      ForeignKey(
        entity = LocationOfInterestEntity::class,
        parentColumns = ["id"],
        childColumns = ["location_of_interest_id"],
        onDelete = ForeignKey.CASCADE
      )
    ],
  indices = [Index("location_of_interest_id")],
)
data class PhotoEntity(
  @ColumnInfo("id") @PrimaryKey val id: String,
  @ColumnInfo("survey_id") val surveyId: String,
  @ColumnInfo("location_of_interest_id") val locationOfInterestId: String,
  @ColumnInfo("job_id") val jobId: String,
  @ColumnInfo("name") val name: String,
  @Embedded("created_") val created: AuditInfoEntity,
  @Embedded("modified_") val modified: AuditInfoEntity,
)
