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
import com.google.android.gnd.model.basemap.tile.TileSet;
import com.google.android.gnd.persistence.local.room.models.TileSetEntityState;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@Entity(tableName = "tile_sources")
public abstract class TileSetEntity {
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
  public abstract TileSetEntityState getState();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "basemap_count")
  public abstract int getOfflineAreaReferenceCount();

  public static TileSet toTileSet(TileSetEntity tileSetEntity) {
    TileSet.Builder tile =
        TileSet.newBuilder()
            .setId(tileSetEntity.getId())
            .setPath(tileSetEntity.getPath())
            .setState(toTileState(tileSetEntity.getState()))
            .setUrl(tileSetEntity.getUrl())
            .setOfflineAreaReferenceCount(tileSetEntity.getOfflineAreaReferenceCount());
    return tile.build();
  }

  private static TileSet.State toTileState(TileSetEntityState state) {
    switch (state) {
      case PENDING:
        return TileSet.State.PENDING;
      case IN_PROGRESS:
        return TileSet.State.IN_PROGRESS;
      case DOWNLOADED:
        return TileSet.State.DOWNLOADED;
      case FAILED:
        return TileSet.State.FAILED;
      default:
        throw new IllegalArgumentException("Unknown tile source state: " + state);
    }
  }

  public static TileSetEntity fromTileSet(TileSet tileSet) {
    TileSetEntity.Builder entity =
        TileSetEntity.builder()
            .setId(tileSet.getId())
            .setPath(tileSet.getPath())
            .setState(toEntityState(tileSet.getState()))
            .setUrl(tileSet.getUrl())
            .setOfflineAreaReferenceCount(tileSet.getOfflineAreaReferenceCount());
    return entity.build();
  }

  private static TileSetEntityState toEntityState(TileSet.State state) {
    switch (state) {
      case PENDING:
        return TileSetEntityState.PENDING;
      case IN_PROGRESS:
        return TileSetEntityState.IN_PROGRESS;
      case FAILED:
        return TileSetEntityState.FAILED;
      case DOWNLOADED:
        return TileSetEntityState.DOWNLOADED;
      default:
        return TileSetEntityState.UNKNOWN;
    }
  }

  public static TileSetEntity create(
      String id, String path, TileSetEntityState state, String url, int offlineAreaReferenceCount) {
    return builder()
        .setId(id)
        .setState(state)
        .setPath(path)
        .setUrl(url)
        .setOfflineAreaReferenceCount(offlineAreaReferenceCount)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_TileSetEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setUrl(String url);

    public abstract Builder setId(String newId);

    public abstract Builder setPath(String newPath);

    public abstract Builder setState(TileSetEntityState newState);

    public abstract Builder setOfflineAreaReferenceCount(int offlineAreaReferenceCount);

    public abstract TileSetEntity build();
  }
}
