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
import org.groundplatform.android.model.mutation.SubmissionMutation
import org.groundplatform.android.persistence.local.room.fields.MutationEntitySyncStatus
import org.groundplatform.android.persistence.local.room.fields.MutationEntityType

/** Representation of a [SubmissionMutation] in local data store. */
@Entity(
  tableName = "submission_mutation",
  foreignKeys =
    [
      ForeignKey(
        entity = LocationOfInterestEntity::class,
        parentColumns = ["id"],
        childColumns = ["location_of_interest_id"],
        onDelete = ForeignKey.CASCADE,
      ),
      ForeignKey(
        entity = SubmissionEntity::class,
        parentColumns = ["id"],
        childColumns = ["submission_id"],
        onDelete = ForeignKey.CASCADE,
      ),
    ],
  indices = [Index("location_of_interest_id"), Index("submission_id")],
)
data class SubmissionMutationEntity(
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
  @ColumnInfo(name = "submission_id") val submissionId: String,
  @ColumnInfo(name = "collection_id") val collectionId: String,
  /**
   * For mutations of type [MutationEntityType.CREATE] and [MutationEntityType.UPDATE], returns a
   * [JSONObject] with the new values of modified submission data, with `null` values representing
   * values which were removed or unset.
   *
   * This method returns `null` for mutation type [MutationEntityType.DELETE].
   */
  @ColumnInfo(name = "deltas") val deltas: String?,
)
