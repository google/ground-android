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
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.basemap.tile.TileSource;
import com.google.android.gnd.persistence.local.room.models.TileEntityState;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@Entity(tableName = "tile_sources")
public abstract class TileSourceEntity {
  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "path")
  public abstract String getPath();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "url")
  public abstract String getUrl();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "state")
  public abstract TileEntityState getState();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "basemap_count")
  public abstract int getBasemapReferenceCount();

  public static TileSource toTileSource(TileSourceEntity tileSourceEntity) {
    TileSource.Builder tile =
        TileSource.newBuilder()
            .setId(tileSourceEntity.getId())
            .setPath(tileSourceEntity.getPath())
            .setState(toTileState(tileSourceEntity.getState()))
            .setUrl(tileSourceEntity.getUrl())
            .setBasemapReferenceCount(tileSourceEntity.getBasemapReferenceCount());
    return tile.build();
  }

  private static TileSource.State toTileState(TileEntityState state) {
    switch (state) {
      case PENDING:
        return TileSource.State.PENDING;
      case IN_PROGRESS:
        return TileSource.State.IN_PROGRESS;
      case DOWNLOADED:
        return TileSource.State.DOWNLOADED;
      case FAILED:
        return TileSource.State.FAILED;
      default:
        throw new IllegalArgumentException("Unknown tile source state: " + state);
    }
  }

  public static TileSourceEntity fromTile(TileSource tileSource) {
    TileSourceEntity.Builder entity =
        TileSourceEntity.builder()
            .setId(tileSource.getId())
            .setPath(tileSource.getPath())
            .setState(toEntityState(tileSource.getState()))
            .setUrl(tileSource.getUrl())
            .setBasemapReferenceCount(tileSource.getBasemapReferenceCount());
    return entity.build();
  }

  private static TileEntityState toEntityState(TileSource.State state) {
    switch (state) {
      case PENDING:
        return TileEntityState.PENDING;
      case IN_PROGRESS:
        return TileEntityState.IN_PROGRESS;
      case FAILED:
        return TileEntityState.FAILED;
      case DOWNLOADED:
        return TileEntityState.DOWNLOADED;
      default:
        return TileEntityState.UNKNOWN;
    }
  }

  public static TileSourceEntity create(
      String id, String path, TileEntityState state, String url, int basemapReferenceCount) {
    return builder()
        .setId(id)
        .setState(state)
        .setPath(path)
        .setUrl(url)
        .setBasemapReferenceCount(basemapReferenceCount)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_TileSourceEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setUrl(String url);

    public abstract Builder setId(String newId);

    public abstract Builder setPath(String newPath);

    public abstract Builder setState(TileEntityState newState);

    public abstract Builder setBasemapReferenceCount(int basemapReferenceCount);

    public abstract TileSourceEntity build();
  }
}
