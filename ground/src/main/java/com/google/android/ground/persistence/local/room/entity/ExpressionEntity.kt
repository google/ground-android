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
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.android.ground.persistence.local.room.fields.ExpressionEntityType

@Entity(
  tableName = "expression",
  foreignKeys =
    [
      ForeignKey(
        entity = ConditionEntity::class,
        parentColumns = ["parent_task_id"],
        childColumns = ["parent_task_id"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("parent_task_id")],
)
data class ExpressionEntity(
  @ColumnInfo(name = "parent_task_id") @PrimaryKey val parentTaskId: String,
  @ColumnInfo(name = "task_id") val taskId: String,
  @ColumnInfo(name = "expression_type") val expressionType: ExpressionEntityType,
  // CSV encoded string.
  @ColumnInfo(name = "option_ids") val optionIds: String?,
)
