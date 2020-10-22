package com.google.android.gnd.model.feature;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class GeoJson {

  public static Builder newBuilder() {
    return new AutoValue_GeoJson.Builder();
  }

  // TODO: Add point and polyline

  public abstract ImmutableList<Polygon> getPolygons();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setPolygons(ImmutableList<Polygon> newPolygons);

    public abstract GeoJson build();
  }
}
