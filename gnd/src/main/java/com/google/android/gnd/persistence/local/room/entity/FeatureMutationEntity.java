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

package com.google.android.gnd.persistence.local.room.entity;

import static androidx.room.ForeignKey.CASCADE;
import static com.google.android.gnd.persistence.local.room.entity.FeatureEntity.formatVertices;
import static com.google.android.gnd.persistence.local.room.entity.FeatureEntity.parseVertices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.google.android.gnd.model.mutation.FeatureMutation;
import com.google.android.gnd.persistence.local.room.models.Coordinates;
import com.google.android.gnd.persistence.local.room.models.MutationEntitySyncStatus;
import com.google.android.gnd.persistence.local.room.models.MutationEntityType;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import java.util.Date;
import java8.util.Optional;

/**
 * Defines how Room persists feature mutations for remote sync in the local db. By default, Room
 * uses the name of object fields and their respective types to determine database column names and
 * types.
 */
@AutoValue
@Entity(
    tableName = "feature_mutation",
    foreignKeys =
        @ForeignKey(
            entity = FeatureEntity.class,
            parentColumns = "id",
            childColumns = "feature_id",
            onDelete = CASCADE),
    indices = {@Index("feature_id")})
public abstract class FeatureMutationEntity extends MutationEntity {

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "feature_id")
  public abstract String getFeatureId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "job_id")
  public abstract String getJobId();

  /** Non-null if the feature's location was updated, null if unchanged. */
  @CopyAnnotations
  @Nullable
  @Embedded
  public abstract Coordinates getNewLocation();

  // Boilerplate generated using Android Studio AutoValue plugin:

  /** Non-empty if a polygon's vertices were updated, null if unchanged. */
  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "polygon_vertices")
  public abstract String getNewPolygonVertices();

  public static FeatureMutationEntity fromMutation(FeatureMutation m) {
    return FeatureMutationEntity.builder()
        .setId(m.getId())
        .setSurveyId(m.getSurveyId())
        .setFeatureId(m.getFeatureId())
        .setJobId(m.getJobId())
        .setNewLocation(m.getLocation().map(Coordinates::fromPoint).orElse(null))
        .setNewPolygonVertices(formatVertices(m.getPolygonVertices()))
        .setType(MutationEntityType.fromMutationType(m.getType()))
        .setSyncStatus(MutationEntitySyncStatus.fromMutationSyncStatus(m.getSyncStatus()))
        .setRetryCount(m.getRetryCount())
        .setLastError(m.getLastError())
        .setUserId(m.getUserId())
        .setClientTimestamp(m.getClientTimestamp().getTime())
        .build();
  }

  public FeatureMutation toMutation() {
    return FeatureMutation.builder()
        .setLocation(Optional.ofNullable(getNewLocation()).map(Coordinates::toPoint))
        .setPolygonVertices(parseVertices(getNewPolygonVertices()))
        .setId(getId())
        .setSurveyId(getSurveyId())
        .setFeatureId(getFeatureId())
        .setJobId(getJobId())
        .setType(getType().toMutationType())
        .setSyncStatus(getSyncStatus().toMutationSyncStatus())
        .setRetryCount(getRetryCount())
        .setLastError(getLastError())
        .setUserId(getUserId())
        .setClientTimestamp(new Date(getClientTimestamp()))
        .build();
  }

  public static FeatureMutationEntity create(
      @Nullable Long id,
      String surveyId,
      String featureId,
      String jobId,
      MutationEntityType type,
      MutationEntitySyncStatus syncStatus,
      @Nullable Coordinates newLocation,
      @Nullable String newPolygonVertices,
      long retryCount,
      @Nullable String lastError,
      String userId,
      long clientTimestamp) {
    return builder()
        .setId(id)
        .setSurveyId(surveyId)
        .setFeatureId(featureId)
        .setJobId(jobId)
        .setType(type)
        .setSyncStatus(syncStatus)
        .setNewLocation(newLocation)
        .setNewPolygonVertices(newPolygonVertices)
        .setRetryCount(retryCount)
        .setLastError(lastError)
        .setUserId(userId)
        .setClientTimestamp(clientTimestamp)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_FeatureMutationEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder extends MutationEntity.Builder<Builder> {

    public abstract Builder setFeatureId(String newFeatureId);

    public abstract Builder setJobId(String newJobId);

    public abstract Builder setNewLocation(@Nullable Coordinates newNewLocation);

    public abstract Builder setNewPolygonVertices(@Nullable String newPolygonVertices);

    public abstract FeatureMutationEntity build();
  }
}
