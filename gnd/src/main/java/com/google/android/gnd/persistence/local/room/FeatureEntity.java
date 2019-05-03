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
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.persistence.shared.FeatureMutation;

/** Representation of a {@link com.google.android.gnd.vo.Feature} in local db. */
@Entity(
    tableName = "feature",
    indices = {@Index("id")})
public class FeatureEntity {

  @NonNull @PrimaryKey public String id;

  @NonNull public EntityState state;

  @NonNull public String projectId;

  @NonNull @Embedded public Coordinates location;

  @NonNull
  static FeatureEntity fromMutation(FeatureMutation m) {
    FeatureEntity fe = new FeatureEntity();
    fe.id = m.getFeatureId();
    fe.projectId = m.getProjectId();
    fe.state = EntityState.DEFAULT;
    m.getNewLocation().ifPresent(l -> fe.location = Coordinates.fromPoint(l));
    return fe;
  }
}
