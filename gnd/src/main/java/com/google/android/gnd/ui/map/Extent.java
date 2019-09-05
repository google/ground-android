package com.google.android.gnd.ui.map;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Extent {
  public enum State {
    SELECTED,
    UNSELECTED
  }

  public abstract String getId();

  public abstract State getState();

  public static Builder newBuilder() {
    return new AutoValue_Extent.Builder();
  }

  public abstract Builder toBuilder();

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
