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
import com.google.android.ground.model.mutation.MediaMutation
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.room.fields.MutationEntityType

/** Persistent representation of mutations applied to user-provided media during data collection. */
@Entity(
  tableName = "media_mutation",
  foreignKeys =
    [
      ForeignKey(
        entity = SubmissionMutationEntity::class,
        parentColumns = ["id"],
        childColumns = ["submission_mutation_id"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("submission_mutation_id")],
)
data class MediaMutationEntity(
  @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long? = 0,
  @ColumnInfo(name = "type") val type: MutationEntityType,
  @ColumnInfo(name = "media_id") val mediaId: String,
  @ColumnInfo(name = "submission_mutation_id") val submissionMutationId: Long,
  @ColumnInfo(name = "state") val syncStatus: MutationEntitySyncStatus,
  @ColumnInfo(name = "retry_count") val retryCount: Long,
  @ColumnInfo(name = "last_error") val lastError: String,
) {
  companion object {
    fun fromMediaMutation(mutation: MediaMutation): MediaMutationEntity {
      requireNotNull(mutation.submissionMutation.id) {
        "associated submission mutation must have a non-null ID"
      }
      return MediaMutationEntity(
        id = mutation.id,
        type = MutationEntityType.fromMutationType(mutation.type),
        mediaId = mutation.mediaId,
        submissionMutationId = mutation.submissionMutation.id,
        syncStatus = MutationEntitySyncStatus.fromMutationSyncStatus(mutation.syncStatus),
        retryCount = mutation.retryCount,
        lastError = mutation.lastError,
      )
    }
  }
}
