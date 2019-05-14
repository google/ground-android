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
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.persistence.shared.FeatureMutation;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

/**
 * Defines how Room persists feature mutations for remote sync in the local db. By default, Room
 * uses the name of object fields and their respective types to determine database column names and
 * types.
 */
@AutoValue
@Entity(
    tableName = "featureMutation",
    foreignKeys =
        @ForeignKey(
            entity = FeatureEntity.class,
            parentColumns = "id",
            childColumns = "featureId",
            onDelete = CASCADE),
    indices = {@Index("featureId")})
public abstract class FeatureMutationEntity {

  @CopyAnnotations
  @PrimaryKey(autoGenerate = true)
  public abstract int getId();

  @CopyAnnotations
  @NonNull
  public abstract String getFeatureId();

  @CopyAnnotations
  @NonNull
  public abstract MutationEntityType getType();

  /** Non-null if the feature's location was updated, null if unchanged. */
  @CopyAnnotations
  @Nullable
  @Embedded
  public abstract Coordinates getNewLocation();

  static FeatureMutationEntity fromMutation(FeatureMutation m) {
    return FeatureMutationEntity.builder()
        .setFeatureId(m.getFeatureId())
        .setNewLocation(m.getNewLocation().map(Coordinates::fromPoint).orElse(null))
        .setType(MutationEntityType.fromMutationType(m.getType()))
        .build();
  }

  // Auto-generated boilerplate.

  public static FeatureMutationEntity create(
      int id, String featureId, MutationEntityType type, Coordinates newLocation) {
    return builder()
        .setId(id)
        .setFeatureId(featureId)
        .setType(type)
        .setNewLocation(newLocation)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_FeatureMutationEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(int newId);

    public abstract Builder setFeatureId(String newFeatureId);

    public abstract Builder setType(MutationEntityType newType);

    public abstract Builder setNewLocation(Coordinates newNewLocation);

    public abstract FeatureMutationEntity build();
  }
}
