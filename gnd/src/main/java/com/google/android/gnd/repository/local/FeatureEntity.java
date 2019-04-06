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

package com.google.android.gnd.repository.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

/** Representation of a {@link com.google.android.gnd.vo.Feature} in local db. */
@AutoValue
@Entity(
    tableName = "feature",
    indices = {@Index("id")})
public abstract class FeatureEntity {
  @CopyAnnotations
  @PrimaryKey
  @ColumnInfo(name = "id")
  @NonNull
  public abstract String getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "state")
  public abstract EntityState getState();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "project_id")
  public abstract String getProjectId();

  @CopyAnnotations
  @Embedded
  public abstract Coordinates getLocation();

  // Auto-generated boilerplate:

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

    public abstract Builder setLocation(Coordinates newCoordinates);

    public abstract FeatureEntity build();
  }
}
