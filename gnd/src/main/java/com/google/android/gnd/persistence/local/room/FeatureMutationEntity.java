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

/**
 * Representation of a {@link com.google.android.gnd.persistence.shared.FeatureMutation} in local
 * data store.
 */
@Entity(
    tableName = "featureMutation",
    foreignKeys =
        @ForeignKey(
            entity = FeatureEntity.class,
            parentColumns = "id",
            childColumns = "featureId",
            onDelete = CASCADE),
    indices = {@Index("featureId")})
public class FeatureMutationEntity {

  @PrimaryKey(autoGenerate = true)
  public int id;

  @NonNull public String featureId;

  @NonNull public MutationEntityType type;

  /** Non-null of the feature's location was updated, null if unchanged. */
  @Nullable @Embedded public Coordinates newLocation;

  static FeatureMutationEntity fromMutation(FeatureMutation m) {
    FeatureMutationEntity me = new FeatureMutationEntity();
    me.featureId = m.getFeatureId();
    me.newLocation = m.getNewLocation().map(Coordinates::fromPoint).orElse(null);
    me.type = MutationEntityType.fromMutationType(m.getType());
    return me;
  }
}
