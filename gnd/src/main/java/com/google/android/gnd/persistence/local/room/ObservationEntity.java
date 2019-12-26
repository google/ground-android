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

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableList;

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

  private static final String TAG = ObservationEntity.class.getSimpleName();

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

  /**
   * Returns a new instance whose state is the same as the current one, but with the specified
   * mutation applied.
   */
  public ObservationEntity applyMutation(ObservationMutation mutation) {
    // TODO: Implement conversion between layers in a consistent way, e.g.in separate
    // converter classes.
    return applyMutations(ImmutableList.of(ObservationMutationEntity.fromMutation(mutation)));
  }

  /**
   * Returns a new instance whose state is the same as the current one, but with the specified
   * mutations applied.
   */
  public ObservationEntity applyMutations(Iterable<ObservationMutationEntity> mutations) {
    Log.v(TAG, "Merging observation " + this + " with mutations " + mutations);
    ObservationEntity.Builder builder = toBuilder();
    for (ObservationMutationEntity mutation : mutations) {
      builder.responsesBuilder().applyDeltas(mutation.getResponseDeltas());
    }
    Log.v(TAG, "Merged observation " + builder.build());
    return builder.build();
  }

  public static ObservationEntity fromObservation(Observation observation) {
    return ObservationEntity.builder()
        .setId(observation.getId())
        .setFormId(observation.getForm().getId())
        .setFeatureId(observation.getFeature().getId())
        .setState(EntityState.DEFAULT)
        .setResponses(observation.getResponses())
        .build();
  }

  public static ObservationEntity fromMutation(ObservationMutation mutation) {
    return ObservationEntity.builder()
        .setId(mutation.getObservationId())
        .setFormId(mutation.getFormId())
        .setFeatureId(mutation.getFeatureId())
        .setState(EntityState.DEFAULT)
        .setResponses(ResponseMap.builder().applyDeltas(mutation.getResponseDeltas()).build())
        .build();
  }

  // TODO(#127): Replace reference to Feature in Observation with featureId and remove feature arg.
  public static Observation toObservation(Feature feature, ObservationEntity observation) {
    // TODO(#127): Replace reference to Form in Observation with formId and remove here.
    // TODO(#127): Replace reference to Project in Observation with projectId and remove here.
    return Observation.newBuilder()
        .setId(observation.getId())
        .setForm(feature.getLayer().getForm(observation.getFormId()).get())
        .setProject(feature.getProject())
        .setFeature(feature)
        .setResponses(observation.getResponses())
        .build();
  }

  public abstract ObservationEntity.Builder toBuilder();

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static ObservationEntity create(
      String id, EntityState state, String featureId, String formId, ResponseMap responses) {
    return builder()
        .setId(id)
        .setState(state)
        .setFeatureId(featureId)
        .setResponses(responses)
        .setFormId(formId)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_ObservationEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setState(EntityState newState);

    public abstract Builder setFeatureId(String newFeatureId);

    public abstract Builder setFormId(String newFormId);

    public abstract Builder setResponses(ResponseMap newResponses);

    public abstract ResponseMap.Builder responsesBuilder();

    public abstract ObservationEntity build();
  }
}
