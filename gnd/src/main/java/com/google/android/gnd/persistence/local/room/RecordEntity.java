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
import com.google.android.gnd.model.observation.RecordMutation;
import com.google.android.gnd.model.observation.ResponseMap;
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
    tableName = "record",
    // Additional index not required for FK constraint since first field in composite index can be
    // used independently.
    indices = {@Index({"feature_id", "form_id", "state"})})
public abstract class RecordEntity {

  private static final String TAG = RecordEntity.class.getSimpleName();

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
   * mutations applied.
   */
  public RecordEntity applyMutations(Iterable<RecordMutationEntity> mutations) {
    Log.v(TAG, "Merging observation " + this + " with mutations " + mutations);
    RecordEntity.Builder builder = toBuilder();
    for (RecordMutationEntity mutation : mutations) {
      builder.responsesBuilder().applyDeltas(mutation.getResponseDeltas());
    }
    Log.v(TAG, "Merged observation " + builder.build());
    return builder.build();
  }

  public static RecordEntity fromRecord(Observation observation) {
    return RecordEntity.builder()
        .setId(observation.getId())
        .setFormId(observation.getForm().getId())
        .setFeatureId(observation.getFeature().getId())
        .setState(EntityState.DEFAULT)
        .setResponses(observation.getResponses())
        .build();
  }

  public static RecordEntity fromMutation(RecordMutation mutation) {
    return RecordEntity.builder()
        .setId(mutation.getRecordId())
        .setFormId(mutation.getFormId())
        .setFeatureId(mutation.getFeatureId())
        .setState(EntityState.DEFAULT)
        .setResponses(ResponseMap.builder().applyDeltas(mutation.getResponseDeltas()).build())
        .build();
  }

  // TODO(#127): Replace reference to Feature in Observation with featureId and remove feature arg.
  public static Observation toRecord(Feature feature, RecordEntity record) {
    // TODO(#127): Replace reference to Form in Observation with formId and remove here.
    // TODO(#127): Replace reference to Project in Observation with projectId and remove here.
    return Observation.newBuilder()
        .setId(record.getId())
        .setForm(feature.getLayer().getForm(record.getFormId()).get())
        .setProject(feature.getProject())
        .setFeature(feature)
        .setResponses(record.getResponses())
        .build();
  }

  public abstract RecordEntity.Builder toBuilder();

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static RecordEntity create(
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
    return new AutoValue_RecordEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setState(EntityState newState);

    public abstract Builder setFeatureId(String newFeatureId);

    public abstract Builder setFormId(String newFormId);

    public abstract Builder setResponses(ResponseMap newResponses);

    public abstract ResponseMap.Builder responsesBuilder();

    public abstract RecordEntity build();
  }
}
