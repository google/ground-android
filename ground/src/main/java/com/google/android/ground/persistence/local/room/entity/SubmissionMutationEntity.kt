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
import com.google.android.ground.model.Survey
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.android.ground.persistence.local.room.converter.ResponseDeltasConverter.fromString
import com.google.android.ground.persistence.local.room.converter.ResponseDeltasConverter.toString
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.room.models.MutationEntityType
import java.util.*

/** Representation of a [SubmissionMutation] in local data store. */
@Entity(
  tableName = "submission_mutation",
  foreignKeys =
    [
      ForeignKey(
        entity = LocationOfInterestEntity::class,
        parentColumns = ["id"],
        childColumns = ["location_of_interest_id"],
        onDelete = ForeignKey.CASCADE
      ),
      ForeignKey(
        entity = SubmissionEntity::class,
        parentColumns = ["id"],
        childColumns = ["submission_id"],
        onDelete = ForeignKey.CASCADE
      )],
  indices = [Index("location_of_interest_id"), Index("submission_id")]
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
  /**
   * For mutations of type [MutationEntityType.CREATE] and [MutationEntityType.UPDATE], returns a
   * [JSONObject] with the new values of modified task responses, with `null` values representing
   * responses that were removed/cleared.
   *
   * This method returns `null` for mutation type [MutationEntityType.DELETE].
   */
  @ColumnInfo(name = "response_deltas") val responseDeltas: String?
) {

  @Throws(LocalDataConsistencyException::class)
  fun toMutation(survey: Survey): SubmissionMutation {
    val job =
      survey.getJob(jobId).orElseThrow {
        LocalDataConsistencyException("Unknown jobId in submission mutation $id")
      }
    return SubmissionMutation.builder()
      .setJob(job)
      .setSubmissionId(submissionId)
      .setResponseDeltas(fromString(job, responseDeltas))
      .setId(id)
      .setSurveyId(surveyId)
      .setLocationOfInterestId(locationOfInterestId)
      .setType(type.toMutationType())
      .setSyncStatus(syncStatus.toMutationSyncStatus())
      .setRetryCount(retryCount)
      .setLastError(lastError)
      .setUserId(userId)
      .setClientTimestamp(Date(clientTimestamp))
      .build()
  }

  companion object {

    fun fromMutation(m: SubmissionMutation): SubmissionMutationEntity =
      SubmissionMutationEntity(
        id = m.id,
        surveyId = m.surveyId,
        locationOfInterestId = m.locationOfInterestId,
        jobId = m.job!!.id,
        submissionId = m.submissionId,
        type = MutationEntityType.fromMutationType(m.type),
        syncStatus = MutationEntitySyncStatus.fromMutationSyncStatus(m.syncStatus),
        responseDeltas = toString(m.responseDeltas),
        retryCount = m.retryCount,
        lastError = m.lastError,
        userId = m.userId,
        clientTimestamp = m.clientTimestamp.time
      )
  }
}
