/*
 * Copyright 2020 Google LLC
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
import androidx.room.Entity;
import androidx.room.Index;
import com.google.auto.value.AutoValue;

/** Entity object that represents an association between offline areas and tile sources. */
@AutoValue
@Entity(
    primaryKeys = {"id", "path"},
    tableName = "offline_area_tile_sources_cross_ref",
    indices = @Index("path"))
public abstract class OfflineAreaTileSourceCrossRef {
  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "id")
  public abstract String getOfflineAreaId();

  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "path")
  public abstract String getTileSourcePath();

  public static Builder builder() {
    return new AutoValue_OfflineAreaTileSourceCrossRef.Builder();
  }

  public static OfflineAreaTileSourceCrossRef create(String offlineAreaId, String tileSourcePath) {
    return builder().setOfflineAreaId(offlineAreaId).setTileSourcePath(tileSourcePath).build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setOfflineAreaId(String id);

    public abstract Builder setTileSourcePath(String path);

    public abstract OfflineAreaTileSourceCrossRef build();
  }
}
