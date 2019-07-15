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
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.RecordMutation;
import com.google.android.gnd.vo.ResponseMap;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

/** Representation of a {@link com.google.android.gnd.vo.Record} in local db. */
@AutoValue
@Entity(
    foreignKeys =
        @ForeignKey(
            entity = FeatureEntity.class,
            parentColumns = "id",
            childColumns = "feature_id",
            onDelete = CASCADE),
    tableName = "record",
    indices = {@Index("id"), @Index("feature_id"), @Index("form_id")})
public abstract class RecordEntity {

  private static final String TAG = RecordEntity.class.getSimpleName();

  @CopyAnnotations
  @PrimaryKey
  @ColumnInfo(name = "id")
  @NonNull
  public abstract String getId();

  /** Returns the id of the feature to which this record applies. */
  @CopyAnnotations
  @ColumnInfo(name = "feature_id")
  @NonNull
  public abstract String getFeatureId();

  /** Returns the id of the form to which this record's responses apply. */
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
    Log.v(TAG, "Merging record " + this + " with mutations " + mutations);
    RecordEntity.Builder builder = toBuilder();
    for (RecordMutationEntity mutation : mutations) {
      builder.responsesBuilder().applyDeltas(mutation.getResponseDeltas());
    }
    Log.v(TAG, "Merged record " + builder.build());
    return builder.build();
  }

  public static RecordEntity fromRecord(Record record) {
    return RecordEntity.builder()
        .setId(record.getId())
        .setFormId(record.getForm().getId())
        .setFeatureId(record.getFeature().getId())
        .setState(EntityState.DEFAULT)
        .setResponses(record.getResponses())
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

  // TODO(#127): Replace reference to Feature in Record with featureId and remove feature arg.
  public static Record toRecord(Feature feature, RecordEntity record) {
    // TODO(#127): Replace reference to Form in Record with formId and remove here.
    // TODO(#127): Replace reference to Project in Record with projectId and remove here.
    return Record.newBuilder()
        .setId(record.getId())
        .setForm(feature.getFeatureType().getForm(record.getFormId()).get())
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
