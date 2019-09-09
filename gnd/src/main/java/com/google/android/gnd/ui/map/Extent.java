package com.google.android.gnd.ui.map;

import androidx.annotation.Nullable;

import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Extent {
  public enum State {
    DOWNLOADED,
    PENDING_DOWNLOAD,
    PENDING_REMOVAL,
    NONE
  }

  public abstract String getId();

  public abstract State getState();

  public static Builder newBuilder() {
    return new AutoValue_Extent.Builder();
  }

  public abstract Builder toBuilder();

  public static Extent fromTile(Tile tile) {
    switch (tile.getState()) {
      case IN_PROGRESS:
        return Extent.newBuilder().setId(tile.getId()).setState(State.PENDING_DOWNLOAD).build();
      case DOWNLOADED:
        return Extent.newBuilder().setId(tile.getId()).setState(State.DOWNLOADED).build();
      case FAILED:
        return Extent.newBuilder().setId(tile.getId()).setState(State.NONE).build();
      case PENDING:
        return Extent.newBuilder().setId(tile.getId()).setState(State.PENDING_DOWNLOAD).build();
      case REMOVED:
        return Extent.newBuilder().setId(tile.getId()).setState(State.NONE).build();
      default:
        return Extent.newBuilder().setId(tile.getId()).setState(State.NONE).build();
    }
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String id);

    public abstract Builder setState(State state);

    public abstract Extent build();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof Extent)) {
      return false;
    }

    Extent extent = (Extent) obj;

    if (extent.getId().equals(this.getId())) {
      return true;
    }

    return super.equals(obj);
  }
}
