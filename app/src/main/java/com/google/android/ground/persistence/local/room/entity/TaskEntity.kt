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
import com.google.android.ground.persistence.local.room.fields.TaskEntityType

@Entity(
  tableName = "task",
  foreignKeys =
    [
      ForeignKey(
        entity = JobEntity::class,
        parentColumns = ["id"],
        childColumns = ["job_id"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("job_id")],
)
data class TaskEntity(
  @ColumnInfo(name = "id") @PrimaryKey val id: String,
  @ColumnInfo(name = "index") val index: Int,
  @ColumnInfo(name = "task_type") val taskType: TaskEntityType,
  @ColumnInfo(name = "label") val label: String?,
  @ColumnInfo(name = "is_required") val isRequired: Boolean,
  @ColumnInfo(name = "job_id") val jobId: String?,
  @ColumnInfo(name = "is_add_loi_task") val isAddLoiTask: Boolean,
)
