package com.google.android.gnd.ui.map;

import androidx.annotation.Nullable;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.Style;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@AutoValue
public abstract class MapPolygon extends MapFeature {

  public static Builder newBuilder() {
    return new AutoValue_MapPolygon.Builder();
  }

  public abstract String getId();

  public abstract ImmutableList<ImmutableSet<Point>> getVertices();

  public abstract Style getStyle();

  // TODO: Stop embedding entire Feature in pins to free up memory. Instead, copy only details
  // relevant to rendering pins and uuid to reference the related Feature.
  @Nullable
  public abstract Feature getFeature();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setVertices(ImmutableList<ImmutableSet<Point>> vertices);

    public abstract Builder setStyle(Style style);

    public abstract Builder setFeature(@Nullable Feature newFeature);

    public abstract MapPolygon build();
  }
}
