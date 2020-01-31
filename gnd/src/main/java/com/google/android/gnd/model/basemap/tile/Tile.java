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

package com.google.android.gnd.model.basemap.tile;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Tile {

  public enum State {
    PENDING,
    IN_PROGRESS,
    DOWNLOADED,
    FAILED
  }

  public abstract String getUrl();

  public abstract int getX();

  public abstract int getY();

  public abstract int getZ();

  public abstract String getId();

  public abstract String getPath();

  public abstract State getState();

  public static Builder newBuilder() {
    return new AutoValue_Tile.Builder();
  }

  public static String pathFromCoords(int x, int y, int z) {
    // Tile ids are stored as x-y-z. Paths must be z-x-y.mbtiles.
    // TODO: Convert tile ids to paths in a less restrictive and less hacky manner.
    // TODO: Move this method to a more appropriate home? We need to perform (and possibly will no
    // matter where the tiles are stored) translation between the tile ID and the file path of the
    // corresponding tile source in remote storage/wherever we pull the source tile from.
    String xstr = String.valueOf(x);
    String ystr = String.valueOf(y);
    String zstr = String.valueOf(z);
    String filename = zstr + "-" + xstr + "-" + ystr;

    return filename + ".mbtiles";
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String id);

    public abstract Builder setUrl(String url);

    public abstract Builder setPath(String path);

    public abstract Builder setX(int x);

    public abstract Builder setY(int y);

    public abstract Builder setZ(int z);

    public abstract Builder setState(State state);

    public abstract Tile build();
  }
}
