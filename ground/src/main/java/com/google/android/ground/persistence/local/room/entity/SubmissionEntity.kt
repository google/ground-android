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
import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.ResponseMap
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.android.ground.persistence.local.room.converter.ResponseMapConverter.fromString
import com.google.android.ground.persistence.local.room.converter.ResponseMapConverter.toString
import com.google.android.ground.persistence.local.room.entity.AuditInfoEntity.Companion.fromObject
import com.google.android.ground.persistence.local.room.entity.AuditInfoEntity.Companion.toObject
import com.google.android.ground.persistence.local.room.models.EntityState

/** Representation of a [Submission] in local db. */
@Entity(
  foreignKeys =
    [
      ForeignKey(
        entity = LocationOfInterestEntity::class,
        parentColumns = ["id"],
        childColumns = ["location_of_interest_id"],
        onDelete = ForeignKey.CASCADE
      )],
  tableName = "submission",
  indices = [Index("location_of_interest_id", "job_id", "state")]
)
data class SubmissionEntity(
  @ColumnInfo(name = "id") @PrimaryKey val id: String,
  @ColumnInfo(name = "location_of_interest_id") val locationOfInterestId: String,
  @ColumnInfo(name = "job_id") val jobId: String,
  @ColumnInfo(name = "state") val state: EntityState,
  @ColumnInfo(name = "responses") val responses: String?,
  @Embedded(prefix = "created_") val created: AuditInfoEntity,
  @Embedded(prefix = "modified_") val lastModified: AuditInfoEntity,
) {

  companion object {
    fun fromSubmission(submission: Submission): SubmissionEntity =
      SubmissionEntity(
        id = submission.id,
        jobId = submission.job.id,
        locationOfInterestId = submission.locationOfInterest.id,
        state = EntityState.DEFAULT,
        responses = toString(submission.responses),
        created = fromObject(submission.created),
        lastModified = fromObject(submission.lastModified)
      )

    fun fromMutation(mutation: SubmissionMutation, created: AuditInfo?): SubmissionEntity {
      val authInfo = fromObject(created!!)
      return SubmissionEntity(
        id = mutation.submissionId,
        jobId = mutation.job!!.id,
        locationOfInterestId = mutation.locationOfInterestId,
        state = EntityState.DEFAULT,
        responses = toString(ResponseMap().copyWithDeltas(mutation.responseDeltas)),
        created = authInfo,
        lastModified = authInfo
      )
    }

    fun toSubmission(loi: LocationOfInterest, submission: SubmissionEntity): Submission {
      val jobId = submission.jobId
      val job = loi.job
      if (job.id != jobId) {
        throw LocalDataConsistencyException(
          "LOI job id ${job.id} does not match submission ${submission.jobId}"
        )
      }
      val id = submission.id
      return Submission(
        id,
        loi.surveyId,
        loi,
        job,
        toObject(submission.created),
        toObject(submission.lastModified),
        fromString(job, submission.responses)
      )
    }
  }
}
