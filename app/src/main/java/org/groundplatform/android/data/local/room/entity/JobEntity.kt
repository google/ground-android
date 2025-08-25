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
package org.groundplatform.android.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "job",
  foreignKeys =
    [
      ForeignKey(
        entity = SurveyEntity::class,
        parentColumns = ["id"],
        childColumns = ["survey_id"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("survey_id")],
)
data class JobEntity(
  @PrimaryKey @ColumnInfo(name = "id") val id: String,
  @ColumnInfo(name = "name") val name: String?,
  @ColumnInfo(name = "survey_id") val surveyId: String?,
  @ColumnInfo(name = "strategy") val strategy: String,
  @Embedded(prefix = "style_") val style: StyleEntity?,
)
