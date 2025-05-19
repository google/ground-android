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
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.groundplatform.android.persistence.local.room.fields.MultipleChoiceEntityType

@Entity(
  tableName = "multiple_choice",
  foreignKeys =
    [
      ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_id"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("task_id")],
)
data class MultipleChoiceEntity(
  @ColumnInfo(name = "task_id") @PrimaryKey val taskId: String,
  @ColumnInfo(name = "type") val type: MultipleChoiceEntityType,
  @ColumnInfo(name = "has_other_option") val hasOtherOption: Boolean,
)
