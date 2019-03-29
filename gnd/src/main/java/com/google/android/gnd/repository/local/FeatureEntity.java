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

/** Representation of a {@link com.google.android.gnd.vo.Feature} in local db. */
// TODO: Convert to AutoValue class.
@Entity(
    tableName = "feature",
    indices = {@Index("id")})
public class FeatureEntity {
  /**
   * Key used in {@link com.google.android.gnd.repository.local.FeatureEditEntity} when edit
   * modified location field.
   */
  public static final String LOCATION_EDIT_KEY = "location";

  @PrimaryKey
  @ColumnInfo(name = "id")
  @NonNull
  public String id;

  @NonNull
  @ColumnInfo(name = "state")
  public EntityState state;

  @NonNull
  @ColumnInfo(name = "project_id")
  public String projectId;

  @Embedded public Coordinates location;

  public static class Coordinates {
    @NonNull
    @ColumnInfo(name = "latitude")
    public double latitude;

    @NonNull
    @ColumnInfo(name = "longitude")
    public double longitude;
  }
}
