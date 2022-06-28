/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.ui.map;

import androidx.annotation.Nullable;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.job.Style;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
// TODO: Rename to MapPolyline for correctness.
public abstract class MapPolygon extends MapFeature {

  public static Builder newBuilder() {
    return new AutoValue_MapPolygon.Builder();
  }

  public abstract String getId();

  public abstract ImmutableList<Point> getVertices();

  public abstract Style getStyle();

  @Nullable
  @Override
  public abstract Feature getFeature();

  public abstract Builder toBuilder();

  public boolean isPolygonComplete() {
    if (getVertices().size() < 4) {
      return false;
    }
    Point first = getFirstVertex();
    Point last = getLastVertex();
    return first != null && first.equals(last);
  }

  @Nullable
  public Point getFirstVertex() {
    ImmutableList<Point> vertices = getVertices();
    return vertices.isEmpty() ? null : vertices.get(0);
  }

  @Nullable
  public Point getLastVertex() {
    ImmutableList<Point> vertices = getVertices();
    return vertices.isEmpty() ? null : vertices.get(vertices.size() - 1);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setVertices(ImmutableList<Point> vertices);

    public abstract Builder setStyle(Style style);

    public abstract Builder setFeature(@Nullable Feature newFeature);

    public abstract MapPolygon build();
  }
}
