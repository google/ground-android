package com.google.android.gnd.model.feature;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@AutoValue
public abstract class Polygon {

  public static Builder newBuilder() {
    return new AutoValue_Polygon.Builder();
  }

  public abstract ImmutableList<ImmutableSet<Point>> getVertices();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setVertices(ImmutableList<ImmutableSet<Point>> newVertices);

    public abstract Polygon build();
  }
}
