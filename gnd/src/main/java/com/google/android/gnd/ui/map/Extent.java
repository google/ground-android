package com.google.android.gnd.ui.map;

import androidx.annotation.Nullable;

import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Extent {
  // TODO: Find a more appropriate name for this enum.
  // State is confusing in this case since it suggests the class also maintains the state of the
  // extent's corresponding tile--that's not the case.
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

  private static State toExtentState(Tile.State state) {
    switch (state) {
      case IN_PROGRESS:
        return State.PENDING_DOWNLOAD;
      case DOWNLOADED:
        return State.DOWNLOADED;
      case PENDING:
        return State.PENDING_DOWNLOAD;
      case REMOVED:
        return State.NONE;
      case FAILED:
        return State.NONE;
      default:
        return State.NONE;
    }
  }

  public static Extent fromTile(Tile tile) {
    return Extent.newBuilder().setId(tile.getId()).setState(toExtentState(tile.getState())).build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String id);

    public abstract Builder setState(State state);

    public abstract Extent build();
  }

  // TODO: Remove this override, it's extraneous.
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
