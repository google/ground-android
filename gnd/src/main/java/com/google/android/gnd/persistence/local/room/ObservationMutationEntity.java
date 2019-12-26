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
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableList;
import org.json.JSONObject;

/** Representation of a {@link ObservationMutation} in local data store. */
@AutoValue
@Entity(
    tableName = "observation_mutation",
    foreignKeys = {
      @ForeignKey(
          entity = FeatureEntity.class,
          parentColumns = "id",
          childColumns = "feature_id",
          onDelete = CASCADE),
      @ForeignKey(
          entity = ObservationEntity.class,
          parentColumns = "id",
          childColumns = "observation_id",
          onDelete = CASCADE)
    },
    indices = {@Index("feature_id"), @Index("observation_id")})
public abstract class ObservationMutationEntity {
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
  @ColumnInfo(name = "form_id")
  public abstract String getFormId();

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
  @ColumnInfo(name = "observation_id")
  public abstract String getObservationId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "type")
  public abstract MutationEntityType getType();

  @CopyAnnotations
  @ColumnInfo(name = "retry_count")
  public abstract long getRetryCount();

  @CopyAnnotations
  @ColumnInfo(name = "last_error")
  @Nullable
  public abstract String getLastError();

  /**
   * For mutations of type {@link MutationEntityType#CREATE} and {@link MutationEntityType#UPDATE},
   * returns a {@link JSONObject} with the new values of modified form responses, with {@code null}
   * values representing responses that were removed/cleared.
   *
   * <p>This method returns {@code null} for mutation type {@link MutationEntityType#DELETE}.
   */
  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "response_deltas")
  public abstract ImmutableList<ResponseDelta> getResponseDeltas();

  public static ObservationMutationEntity create(
      long id,
      String projectId,
      String featureId,
      String featureTypeId,
      String formId,
      String observationId,
      MutationEntityType type,
      ImmutableList<ResponseDelta> responseDeltas,
      long retryCount,
      @Nullable String lastError) {
    return builder()
        .setId(id)
        .setProjectId(projectId)
        .setFeatureId(featureId)
        .setFeatureTypeId(featureTypeId)
        .setFormId(formId)
        .setObservationId(observationId)
        .setType(type)
        .setResponseDeltas(responseDeltas)
        .setRetryCount(retryCount)
        .setLastError(lastError)
        .build();
  }

  public static ObservationMutationEntity fromMutation(ObservationMutation m) {
    return ObservationMutationEntity.builder()
        .setId(m.getId())
        .setProjectId(m.getProjectId())
        .setFeatureId(m.getFeatureId())
        .setFeatureTypeId(m.getLayerId())
        .setFormId(m.getFormId())
        .setObservationId(m.getObservationId())
        .setType(MutationEntityType.fromMutationType(m.getType()))
        .setResponseDeltas(m.getResponseDeltas())
        .setRetryCount(m.getRetryCount())
        .setLastError(m.getLastError())
        .build();
  }

  public ObservationMutation toMutation() {
    return ObservationMutation.builder()
        .setId(getId())
        .setProjectId(getProjectId())
        .setFeatureId(getFeatureId())
        .setLayerId(getFeatureTypeId())
        .setFormId(getFormId())
        .setObservationId(getObservationId())
        .setType(getType().toMutationType())
        .setResponseDeltas(getResponseDeltas())
        .setRetryCount(getRetryCount())
        .setLastError(getLastError())
        .build();
  }

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static Builder builder() {
    return new AutoValue_ObservationMutationEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(@Nullable Long newId);

    public abstract Builder setProjectId(@Nullable String newProjectId);

    public abstract Builder setFeatureId(@Nullable String newFeatureId);

    public abstract Builder setFeatureTypeId(@Nullable String newFeatureTypeId);

    public abstract Builder setFormId(@Nullable String newFormId);

    public abstract Builder setObservationId(String newObservationId);

    public abstract Builder setType(MutationEntityType newType);

    public abstract Builder setResponseDeltas(ImmutableList<ResponseDelta> newResponseDeltas);

    public abstract Builder setRetryCount(long newRetryCount);

    public abstract Builder setLastError(@Nullable String newLastError);

    public abstract ObservationMutationEntity build();
  }
}
