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

package com.google.android.ground.persistence.local.room.entity;

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.ground.model.AuditInfo;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.mutation.SubmissionMutation;
import com.google.android.ground.model.submission.ResponseMap;
import com.google.android.ground.model.submission.Submission;
import com.google.android.ground.persistence.local.LocalDataConsistencyException;
import com.google.android.ground.persistence.local.room.converter.ResponseMapConverter;
import com.google.android.ground.persistence.local.room.models.EntityState;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

/** Representation of a {@link Submission} in local db. */
@AutoValue
@Entity(
    foreignKeys =
        @ForeignKey(
            entity = LocationOfInterestEntity.class,
            parentColumns = "id",
            childColumns = "location_of_interest_id",
            onDelete = CASCADE),
    tableName = "submission",
    // Additional index not required for FK constraint since first field in composite index can be
    // used independently.
    indices = {@Index({"location_of_interest_id", "job_id", "state"})})
public abstract class SubmissionEntity {

  @CopyAnnotations
  @PrimaryKey
  @ColumnInfo(name = "id")
  @NonNull
  public abstract String getId();

  /** Returns the id of the location of interest to which this submission applies. */
  @CopyAnnotations
  @ColumnInfo(name = "location_of_interest_id")
  @NonNull
  public abstract String getLocationOfInterestId();

  /** Returns the id of the job to which this submission's responses apply. */
  @CopyAnnotations
  @ColumnInfo(name = "job_id")
  @NonNull
  public abstract String getJobId();

  @CopyAnnotations
  @ColumnInfo(name = "state")
  @NonNull
  public abstract EntityState getState();

  /**
   * Returns a JSON object containing user responses keyed by their respective taskId in the job
   * identified by jobId. Returns null if no responses have been provided.
   */
  @CopyAnnotations
  @ColumnInfo(name = "responses")
  @Nullable
  public abstract String getResponses();

  @CopyAnnotations
  @NonNull
  @Embedded(prefix = "created_")
  public abstract AuditInfoEntity getCreated();

  @CopyAnnotations
  @NonNull
  @Embedded(prefix = "modified_")
  public abstract AuditInfoEntity getLastModified();

  public static SubmissionEntity fromSubmission(Submission submission) {
    return SubmissionEntity.builder()
        .setId(submission.getId())
        .setJobId(submission.getJob().getId())
        .setLocationOfInterestId(submission.getLocationOfInterest().getId())
        .setState(EntityState.DEFAULT)
        .setResponses(ResponseMapConverter.toString(submission.getResponses()))
        .setCreated(AuditInfoEntity.fromObject(submission.getCreated()))
        .setLastModified(AuditInfoEntity.fromObject(submission.getLastModified()))
        .build();
  }

  public static SubmissionEntity fromMutation(SubmissionMutation mutation, AuditInfo created) {
    AuditInfoEntity authInfo = AuditInfoEntity.fromObject(created);
    return SubmissionEntity.builder()
        .setId(mutation.getSubmissionId())
        .setJobId(mutation.getJob().getId())
        .setLocationOfInterestId(mutation.getLocationOfInterestId())
        .setState(EntityState.DEFAULT)
        .setResponses(
            ResponseMapConverter.toString(
                ResponseMap.builder().applyDeltas(mutation.getResponseDeltas()).build()))
        .setCreated(authInfo)
        .setLastModified(authInfo)
        .build();
  }

  public static Submission toSubmission(LocationOfInterest loi, SubmissionEntity submission) {
    String jobId = submission.getJobId();
    Job job = loi.getJob();
    if (!job.getId().equals(jobId)) {
      throw new LocalDataConsistencyException(
          "LOI job id " + job.getId() + " does not match submission " + submission.getJobId());
    }
    String id = submission.getId();
    return new Submission(
        id,
        loi.getSurvey().getId(),
        loi,
        job,
        AuditInfoEntity.toObject(submission.getCreated()),
        AuditInfoEntity.toObject(submission.getLastModified()),
        ResponseMapConverter.fromString(job, submission.getResponses()));
  }

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static SubmissionEntity create(
      String id,
      String locationOfInterestId,
      String jobId,
      EntityState state,
      String responses,
      AuditInfoEntity created,
      AuditInfoEntity lastModified) {
    return builder()
        .setId(id)
        .setLocationOfInterestId(locationOfInterestId)
        .setJobId(jobId)
        .setState(state)
        .setResponses(responses)
        .setCreated(created)
        .setLastModified(lastModified)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_SubmissionEntity.Builder();
  }

  public abstract SubmissionEntity.Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setLocationOfInterestId(String newLocationOfInterestId);

    public abstract Builder setJobId(String newJobId);

    public abstract Builder setState(EntityState newState);

    public abstract Builder setResponses(@Nullable String newResponses);

    public abstract Builder setCreated(AuditInfoEntity newCreated);

    public abstract Builder setLastModified(AuditInfoEntity newLastModified);

    public abstract SubmissionEntity build();
  }
}
