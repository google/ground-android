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

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.persistence.local.room.models.Coordinates;
import com.google.android.gnd.persistence.local.room.models.EntityState;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

/**
 * Defines how Room persists features in the local db. By default, Room uses the name of object
 * fields and their respective types to determine database column names and types.
 */
@AutoValue
@Entity(
    tableName = "feature",
    indices = {@Index("project_id")})
public abstract class FeatureEntity {
  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "project_id")
  public abstract String getProjectId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "feature_type_id")
  public abstract String getFeatureTypeId();

  // TODO: Rename to DeletionState.
  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "state")
  public abstract EntityState getState();

  @CopyAnnotations
  @NonNull
  @Embedded
  public abstract Coordinates getLocation();

  @CopyAnnotations
  @NonNull
  @Embedded(prefix = "created_")
  public abstract AuditInfoEntity getCreated();

  @CopyAnnotations
  @NonNull
  @Embedded(prefix = "modified_")
  public abstract AuditInfoEntity getLastModified();

  @NonNull
  public static FeatureEntity fromMutation(FeatureMutation mutation, AuditInfo created) {
    AuditInfoEntity authInfo = AuditInfoEntity.fromObject(created);
    FeatureEntity.Builder entity =
        FeatureEntity.builder()
            .setId(mutation.getFeatureId())
            .setProjectId(mutation.getProjectId())
            .setFeatureTypeId(mutation.getLayerId())
            .setState(EntityState.DEFAULT)
            .setCreated(authInfo)
            .setLastModified(authInfo);
    mutation.getNewLocation().map(Coordinates::fromPoint).ifPresent(entity::setLocation);
    return entity.build();
  }

  public static FeatureEntity fromFeature(Feature feature) {
    FeatureEntity.Builder entity =
        FeatureEntity.builder()
            .setId(feature.getId())
            .setProjectId(feature.getProject().getId())
            .setFeatureTypeId(feature.getLayer().getId())
            .setLocation(Coordinates.fromPoint(feature.getPoint()))
            .setState(EntityState.DEFAULT)
            .setCreated(AuditInfoEntity.fromObject(feature.getCreated()))
            .setLastModified(AuditInfoEntity.fromObject(feature.getLastModified()));
    return entity.build();
  }

  // TODO(#127): Decouple from Project and remove 2nd argument.
  public static Feature toFeature(FeatureEntity featureEntity, Project project) {
    return Feature.newBuilder()
        .setId(featureEntity.getId())
        .setProject(project)
        .setLayer(project.getLayer(featureEntity.getFeatureTypeId()).get())
        .setPoint(featureEntity.getLocation().toPoint())
        .setCreated(AuditInfoEntity.toObject(featureEntity.getCreated()))
        .setLastModified(AuditInfoEntity.toObject(featureEntity.getLastModified()))
        .build();
  }

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static FeatureEntity create(
      String id,
      String projectId,
      String featureTypeId,
      EntityState state,
      Coordinates location,
      AuditInfoEntity created,
      AuditInfoEntity lastModified) {
    return builder()
        .setId(id)
        .setProjectId(projectId)
        .setFeatureTypeId(featureTypeId)
        .setState(state)
        .setLocation(location)
        .setCreated(created)
        .setLastModified(lastModified)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_FeatureEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setProjectId(String newProjectId);

    public abstract Builder setFeatureTypeId(String newFeatureTypeId);

    public abstract Builder setState(EntityState newState);

    public abstract Builder setLocation(Coordinates newLocation);

    public abstract Builder setCreated(AuditInfoEntity newCreated);

    public abstract Builder setLastModified(AuditInfoEntity newLastModified);

    public abstract FeatureEntity build();
  }
}
