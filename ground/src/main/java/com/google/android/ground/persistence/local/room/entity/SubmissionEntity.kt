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

import androidx.room.*
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.persistence.local.room.fields.EntityDeletionState

/** Representation of a [Submission] in local db. */
@Entity(
  foreignKeys =
    [
      ForeignKey(
        entity = LocationOfInterestEntity::class,
        parentColumns = ["id"],
        childColumns = ["location_of_interest_id"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  tableName = "submission",
  indices = [Index("location_of_interest_id", "job_id", "state")],
)
data class SubmissionEntity(
  @ColumnInfo(name = "id") @PrimaryKey val id: String,
  @ColumnInfo(name = "location_of_interest_id") val locationOfInterestId: String,
  @ColumnInfo(name = "job_id") val jobId: String,
  @ColumnInfo(name = "state") val deletionState: EntityDeletionState,
  @ColumnInfo(name = "data") val data: String?,
  @Embedded(prefix = "created_") val created: AuditInfoEntity,
  @Embedded(prefix = "modified_") val lastModified: AuditInfoEntity,
)
