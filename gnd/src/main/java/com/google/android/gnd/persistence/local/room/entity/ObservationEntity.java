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

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.persistence.local.LocalDataConsistencyException;
import com.google.android.gnd.persistence.local.room.models.EntityState;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

/** Representation of a {@link Observation} in local db. */
@AutoValue
@Entity(
    foreignKeys =
        @ForeignKey(
            entity = FeatureEntity.class,
            parentColumns = "id",
            childColumns = "feature_id",
            onDelete = CASCADE),
    tableName = "observation",
    // Additional index not required for FK constraint since first field in composite index can be
    // used independently.
    indices = {@Index({"feature_id", "form_id", "state"})})
public abstract class ObservationEntity {

  @CopyAnnotations
  @PrimaryKey
  @ColumnInfo(name = "id")
  @NonNull
  public abstract String getId();

  /** Returns the id of the feature to which this observation applies. */
  @CopyAnnotations
  @ColumnInfo(name = "feature_id")
  @NonNull
  public abstract String getFeatureId();

  /** Returns the id of the form to which this observation's responses apply. */
  @CopyAnnotations
  @ColumnInfo(name = "form_id")
  @NonNull
  public abstract String getFormId();

  @CopyAnnotations
  @ColumnInfo(name = "state")
  @NonNull
  public abstract EntityState getState();

  /**
   * Returns a JSON object containing user responses keyed by their respective elementId in the form
   * identified by formId. Returns an empty JSON object if no responses have been provided.
   */
  @CopyAnnotations
  @ColumnInfo(name = "responses")
  @NonNull
  public abstract ResponseMap getResponses();

  @CopyAnnotations
  @NonNull
  @Embedded(prefix = "created_")
  public abstract AuditInfoEntity getCreated();

  @CopyAnnotations
  @NonNull
  @Embedded(prefix = "modified_")
  public abstract AuditInfoEntity getLastModified();

  public static ObservationEntity fromObservation(Observation observation) {
    return ObservationEntity.builder()
        .setId(observation.getId())
        .setFormId(observation.getForm().getId())
        .setFeatureId(observation.getFeature().getId())
        .setState(EntityState.DEFAULT)
        .setResponses(observation.getResponses())
        .setCreated(AuditInfoEntity.fromObject(observation.getCreated()))
        .setLastModified(AuditInfoEntity.fromObject(observation.getLastModified()))
        .build();
  }

  public static ObservationEntity fromMutation(ObservationMutation mutation, AuditInfo created) {
    AuditInfoEntity authInfo = AuditInfoEntity.fromObject(created);
    return ObservationEntity.builder()
        .setId(mutation.getObservationId())
        .setFormId(mutation.getFormId())
        .setFeatureId(mutation.getFeatureId())
        .setState(EntityState.DEFAULT)
        .setResponses(ResponseMap.builder().applyDeltas(mutation.getResponseDeltas()).build())
        .setCreated(authInfo)
        .setLastModified(authInfo)
        .build();
  }

  public static Observation toObservation(Feature feature, ObservationEntity observation) {
    String id = observation.getId();
    String formId = observation.getFormId();
    Form form =
        feature
            .getLayer()
            .getForm(formId)
            .orElseThrow(
                () ->
                    new LocalDataConsistencyException(
                        "Unknown formId " + formId + " in observation " + id));
    return Observation.newBuilder()
        .setId(id)
        .setForm(form)
        .setProject(feature.getProject())
        .setFeature(feature)
        .setResponses(observation.getResponses())
        .setCreated(AuditInfoEntity.toObject(observation.getCreated()))
        .setLastModified(AuditInfoEntity.toObject(observation.getLastModified()))
        .build();
  }

  public abstract ObservationEntity.Builder toBuilder();

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static ObservationEntity create(
      String id,
      String featureId,
      String formId,
      EntityState state,
      ResponseMap responses,
      AuditInfoEntity created,
      AuditInfoEntity lastModified) {
    return builder()
        .setId(id)
        .setFeatureId(featureId)
        .setFormId(formId)
        .setState(state)
        .setResponses(responses)
        .setCreated(created)
        .setLastModified(lastModified)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_ObservationEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setFeatureId(String newFeatureId);

    public abstract Builder setFormId(String newFormId);

    public abstract Builder setState(EntityState newState);

    public abstract Builder setResponses(ResponseMap newResponses);

    public abstract ResponseMap.Builder responsesBuilder();

    public abstract Builder setCreated(AuditInfoEntity newCreated);

    public abstract Builder setLastModified(AuditInfoEntity newLastModified);

    /** Applies the specified mutation to this builder. */
    public Builder applyMutation(ObservationMutationEntity mutation) {
      responsesBuilder().applyDeltas(mutation.getResponseDeltas());
      return this;
    }

    public abstract ObservationEntity build();
  }
}
