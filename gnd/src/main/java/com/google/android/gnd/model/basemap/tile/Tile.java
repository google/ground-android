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

    public static Builder newBuilder() {return new AutoValue_Tile.Builder();}

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setId(String id);
        public abstract Builder setPath(String path);
        public abstract Builder setState(State state);

        public abstract Tile build();
    }
}