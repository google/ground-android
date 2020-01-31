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
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@Entity(tableName = "tile")
public abstract class TileEntity {
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
  @ColumnInfo(name = "x")
  public abstract int getX();

  @CopyAnnotations
  @ColumnInfo(name = "y")
  public abstract int getY();

  @CopyAnnotations
  @ColumnInfo(name = "z")
  public abstract int getZ();

  public static Tile toTile(TileEntity tileEntity) {
    Tile.Builder tile =
        Tile.newBuilder()
            .setId(tileEntity.getId())
            .setPath(tileEntity.getPath())
            .setState(toTileState(tileEntity.getState()))
            .setX(tileEntity.getX())
            .setY(tileEntity.getY())
            .setZ(tileEntity.getZ())
            .setUrl(tileEntity.getUrl());
    return tile.build();
  }

  private static Tile.State toTileState(TileEntityState state) {
    switch (state) {
      case PENDING:
        return Tile.State.PENDING;
      case IN_PROGRESS:
        return Tile.State.IN_PROGRESS;
      case DOWNLOADED:
        return Tile.State.DOWNLOADED;
      case FAILED:
        return Tile.State.FAILED;
      default:
        throw new IllegalArgumentException("Unknown tile state: " + state);
    }
  }

  public static TileEntity fromTile(Tile tile) {
    TileEntity.Builder entity =
        TileEntity.builder()
            .setId(tile.getId())
            .setPath(tile.getPath())
            .setState(toEntityState(tile.getState()))
            .setUrl(tile.getUrl())
            .setX(tile.getX())
            .setY(tile.getY())
            .setZ(tile.getZ());
    return entity.build();
  }

  private static TileEntityState toEntityState(Tile.State state) {
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

  public static TileEntity create(
      String id, String path, TileEntityState state, String url, int x, int y, int z) {
    return builder()
        .setId(id)
        .setState(state)
        .setPath(path)
        .setUrl(url)
        .setX(x)
        .setY(y)
        .setZ(z)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_TileEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setUrl(String url);

    public abstract Builder setX(int x);

    public abstract Builder setY(int y);

    public abstract Builder setZ(int z);

    public abstract Builder setId(String newId);

    public abstract Builder setPath(String newPath);

    public abstract Builder setState(TileEntityState newState);

    public abstract TileEntity build();
  }
}
