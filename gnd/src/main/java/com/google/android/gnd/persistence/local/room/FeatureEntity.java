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

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.persistence.shared.FeatureMutation;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

/**
 * Defines how Room persists features in the local db. By default, Room uses the name of object
 * fields and their respective types to determine database column names and types.
 */
@AutoValue
@Entity(
    tableName = "feature",
    indices = {@Index("id"), @Index("project_id")})
public abstract class FeatureEntity {
  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  // TODO: Rename to DeletionState.
  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "state")
  public abstract EntityState getState();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "project_id")
  public abstract String getProjectId();

  @CopyAnnotations
  @NonNull
  @Embedded
  public abstract Coordinates getLocation();

  @NonNull
  static FeatureEntity fromMutation(FeatureMutation mutation) {
    FeatureEntity.Builder entity =
        FeatureEntity.builder()
            .setId(mutation.getFeatureId())
            .setProjectId(mutation.getProjectId())
            .setState(EntityState.DEFAULT);
    mutation.getNewLocation().map(Coordinates::fromPoint).ifPresent(entity::setLocation);
    return entity.build();
  }

  // Auto-generated boilerplate.

  public static FeatureEntity create(
      String id, EntityState state, String projectId, Coordinates location) {
    return builder()
        .setId(id)
        .setState(state)
        .setProjectId(projectId)
        .setLocation(location)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_FeatureEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setState(EntityState newState);

    public abstract Builder setProjectId(String newProjectId);

    public abstract Builder setLocation(Coordinates newLocation);

    public abstract FeatureEntity build();
  }
}
