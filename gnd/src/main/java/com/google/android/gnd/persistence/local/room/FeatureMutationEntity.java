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

package com.google.android.gnd.persistence.local.room;

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.persistence.shared.FeatureMutation;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
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
    indices = {@Index("id"), @Index("feature_id")})
public abstract class FeatureMutationEntity {
  // TODO: Refactor common attributes in MutationEntity base case.

  @CopyAnnotations
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  @Nullable
  public abstract Long getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "project_id")
  public abstract String getProjectId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "feature_id")
  public abstract String getFeatureId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "feature_type_id")
  public abstract String getFeatureTypeId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "type")
  public abstract MutationEntityType getType();

  /** Non-null if the feature's location was updated, null if unchanged. */
  @CopyAnnotations
  @Nullable
  @Embedded
  public abstract Coordinates getNewLocation();

  @CopyAnnotations
  @ColumnInfo(name = "retry_count")
  public abstract long getRetryCount();

  @CopyAnnotations
  @ColumnInfo(name = "last_error")
  @Nullable
  public abstract String getLastError();

  static FeatureMutationEntity fromMutation(FeatureMutation m) {
    return FeatureMutationEntity.builder()
        .setId(m.getId())
        .setProjectId(m.getProjectId())
        .setFeatureId(m.getFeatureId())
        .setFeatureTypeId(m.getFeatureTypeId())
        .setNewLocation(m.getNewLocation().map(Coordinates::fromPoint).orElse(null))
        .setType(MutationEntityType.fromMutationType(m.getType()))
        .setRetryCount(m.getRetryCount())
        .setLastError(m.getLastError())
        .build();
  }

  public FeatureMutation toMutation() {
    return FeatureMutation.builder()
        .setId(getId())
        .setProjectId(getProjectId())
        .setFeatureId(getFeatureId())
        .setFeatureTypeId(getFeatureTypeId())
        .setNewLocation(Optional.ofNullable(getNewLocation().toPoint()))
        .setType(getType().toMutationType())
        .setRetryCount(getRetryCount())
        .setLastError(getLastError())
        .build();
  }

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static FeatureMutationEntity create(
      @Nullable Long id,
      String projectId,
      String featureId,
      String featureTypeId,
      MutationEntityType type,
      Coordinates newLocation,
      long retryCount,
      @Nullable String lastError) {
    return builder()
        .setId(id)
        .setProjectId(projectId)
        .setFeatureId(featureId)
        .setFeatureTypeId(featureTypeId)
        .setType(type)
        .setNewLocation(newLocation)
        .setRetryCount(retryCount)
        .setLastError(lastError)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_FeatureMutationEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(@Nullable Long newId);

    public abstract Builder setProjectId(String newProjectId);

    public abstract Builder setFeatureId(String newFeatureId);

    public abstract Builder setFeatureTypeId(String newFeatureTypeId);

    public abstract Builder setType(MutationEntityType newType);

    public abstract Builder setNewLocation(Coordinates newNewLocation);

    public abstract Builder setRetryCount(long newRetryCount);

    public abstract Builder setLastError(@Nullable String newLastError);

    public abstract FeatureMutationEntity build();
  }
}
