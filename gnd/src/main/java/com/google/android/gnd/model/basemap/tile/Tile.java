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

  public abstract String getId();

  public abstract String getPath();

  public abstract State getState();

  public static Builder newBuilder() {
    return new AutoValue_Tile.Builder();
  }

  public static String pathFromId(String tileId) {
    // Tile ids are stored as x-y-z. Paths must be z-x-y.mbtiles.
    // TODO: Convert tile ids to paths in a less restrictive and less hacky manner.
    String[] fields = tileId.replaceAll("[()]", "").split(", ");
    String filename = fields[2] + "-" + fields[0] + "-" + fields[1];

    return filename + ".mbtiles";
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String id);

    public abstract Builder setPath(String path);

    public abstract Builder setState(State state);

    public abstract Tile build();
  }
}
