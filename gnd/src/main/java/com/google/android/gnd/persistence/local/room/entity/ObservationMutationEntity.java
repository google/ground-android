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
import static androidx.room.ForeignKey.SET_DEFAULT;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.persistence.local.LocalDataConsistencyException;
import com.google.android.gnd.persistence.local.room.converter.ResponseDeltasConverter;
import com.google.android.gnd.persistence.local.room.models.MutationEntitySyncStatus;
import com.google.android.gnd.persistence.local.room.models.MutationEntityType;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import java.util.Date;
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
          onDelete = SET_DEFAULT),
      @ForeignKey(
          entity = ObservationEntity.class,
          parentColumns = "id",
          childColumns = "observation_id",
          onDelete = SET_DEFAULT)
    },
    indices = {@Index("feature_id"), @Index("observation_id")})
public abstract class ObservationMutationEntity extends MutationEntity {
  @CopyAnnotations
  @ColumnInfo(name = "form_id")
  public abstract String getFormId();

  @CopyAnnotations
  @ColumnInfo(name = "feature_id")
  public abstract String getFeatureId();

  @CopyAnnotations
  @ColumnInfo(name = "layer_id")
  public abstract String getLayerId();

  @CopyAnnotations
  @ColumnInfo(name = "observation_id")
  public abstract String getObservationId();

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
  public abstract String getResponseDeltas();

  public static ObservationMutationEntity create(
      long id,
      String projectId,
      String featureId,
      String layerId,
      String formId,
      String observationId,
      MutationEntityType type,
      MutationEntitySyncStatus syncStatus,
      String responseDeltas,
      long retryCount,
      @Nullable String lastError,
      @Nullable String userId,
      long clientTimestamp) {
    return builder()
        .setId(id)
        .setProjectId(projectId)
        .setFeatureId(featureId)
        .setLayerId(layerId)
        .setFormId(formId)
        .setObservationId(observationId)
        .setType(type)
        .setSyncStatus(syncStatus)
        .setResponseDeltas(responseDeltas)
        .setRetryCount(retryCount)
        .setLastError(lastError)
        .setUserId(userId)
        .setClientTimestamp(clientTimestamp)
        .build();
  }

  public static ObservationMutationEntity fromMutation(ObservationMutation m) {
    return ObservationMutationEntity.builder()
        .setId(m.getId())
        .setProjectId(m.getProjectId())
        .setFeatureId(m.getFeatureId())
        .setLayerId(m.getLayerId())
        .setFormId(m.getForm().getId())
        .setObservationId(m.getObservationId())
        .setType(MutationEntityType.fromMutationType(m.getType()))
        .setSyncStatus(MutationEntitySyncStatus.fromMutationSyncStatus(m.getSyncStatus()))
        .setResponseDeltas(ResponseDeltasConverter.toString(m.getResponseDeltas()))
        .setRetryCount(m.getRetryCount())
        .setLastError(m.getLastError())
        .setUserId(m.getUserId())
        .setClientTimestamp(m.getClientTimestamp().getTime())
        .build();
  }

  public ObservationMutation toMutation(Project project) throws LocalDataConsistencyException {
    Layer layer =
        project
            .getLayer(getLayerId())
            .orElseThrow(
                () ->
                    new LocalDataConsistencyException(
                        "Unknown layerId in  in observation mutation " + getId()));
    Form form =
        layer
            .getForm(getFormId())
            .orElseThrow(
                () ->
                    new LocalDataConsistencyException(
                        "Unknown formId in observation mutation " + getId()));
    return ObservationMutation.builder()
        .setId(getId())
        .setProjectId(getProjectId())
        .setFeatureId(getFeatureId())
        .setLayerId(getLayerId())
        .setForm(form)
        .setObservationId(getObservationId())
        .setType(getType().toMutationType())
        .setSyncStatus(getSyncStatus().toMutationSyncStatus())
        .setResponseDeltas(ResponseDeltasConverter.fromString(form, getResponseDeltas()))
        .setRetryCount(getRetryCount())
        .setLastError(getLastError())
        .setUserId(getUserId())
        .setClientTimestamp(new Date(getClientTimestamp()))
        .build();
  }

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static Builder builder() {
    return new AutoValue_ObservationMutationEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder extends MutationEntity.Builder<Builder> {

    public abstract Builder setFeatureId(String newFeatureId);

    public abstract Builder setLayerId(String newLayerId);

    public abstract Builder setFormId(String newFormId);

    public abstract Builder setObservationId(String newObservationId);

    public abstract Builder setResponseDeltas(String newResponseDeltas);

    public abstract ObservationMutationEntity build();
  }
}
