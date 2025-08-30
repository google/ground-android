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
package org.groundplatform.android.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.groundplatform.android.model.submission.DraftSubmission

/** Representation of a [DraftSubmission] in local db. */
@Entity(tableName = "draft_submission", indices = [Index("loi_id", "job_id", "survey_id")])
data class DraftSubmissionEntity(
  @ColumnInfo(name = "id") @PrimaryKey val id: String,
  @ColumnInfo(name = "job_id") val jobId: String,
  @ColumnInfo(name = "loi_id") val loiId: String?,
  @ColumnInfo(name = "survey_id") val surveyId: String,
  @ColumnInfo(name = "deltas") val deltas: String?,
  @ColumnInfo(name = "loi_name") val loiName: String?,
  @ColumnInfo(name = "current_task_id") val currentTaskId: String?,
)
