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
package org.groundplatform.android.persistence.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.groundplatform.android.model.locationofinterest.LoiProperties
import org.groundplatform.android.persistence.local.room.fields.EntityDeletionState

/**
 * Defines how Room persists LOIs in the local db. By default, Room uses the name of object fields
 * and their respective types to determine database column names and types.
 */
@Entity(
  tableName = "location_of_interest",
  foreignKeys =
    [
      ForeignKey(
        entity = JobEntity::class,
        parentColumns = ["id"],
        childColumns = ["job_id"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("survey_id"), Index("job_id")],
)
data class LocationOfInterestEntity(
  @ColumnInfo(name = "id") @PrimaryKey val id: String,
  @ColumnInfo(name = "survey_id") val surveyId: String,
  @ColumnInfo(name = "job_id") val jobId: String,
  @ColumnInfo(name = "state") val deletionState: EntityDeletionState,
  @Embedded(prefix = "created_") val created: AuditInfoEntity,
  @Embedded(prefix = "modified_") val lastModified: AuditInfoEntity,
  val geometry: GeometryWrapper?,
  val customId: String,
  val submissionCount: Int,
  val properties: LoiProperties,
  val isPredefined: Boolean?,
)
