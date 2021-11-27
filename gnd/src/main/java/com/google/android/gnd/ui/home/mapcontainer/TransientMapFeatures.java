/*
 * Copyright 2021 Google LLC
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

package com.google.android.gnd.ui.home.mapcontainer;

import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.Style;
import com.google.android.gnd.ui.map.MapFeature;
import com.google.android.gnd.ui.map.MapPin;
import com.google.android.gnd.ui.map.MapPolygon;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/** Helper class to generate ephemeral {@link MapFeature}. Used when adding/editing features. */
public final class TransientMapFeatures {

  private TransientMapFeatures() {}

  private static MapFeature toMapPin(String id, Point point, Style style) {
    return MapPin.newBuilder().setId(id).setPosition(point).setStyle(style).build();
  }

  /**
   * Returns a set of {@link MapFeature} to be drawn on map for the given {@link MapPolygon}.
   *
   * <p>TODO: Use different marker style for ephemeral markers.
   *
   * <p>Includes itself and adds a {@link MapPin} for each vertex.
   */
  public static ImmutableSet<MapFeature> forMapPolygon(MapPolygon mapPolygon) {
    ImmutableList<Point> vertices = mapPolygon.getVertices();

    if (vertices.isEmpty()) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<MapFeature> builder = ImmutableSet.builder();
    builder.add(mapPolygon);
    builder.addAll(
        stream(vertices)
            .map(point -> toMapPin(mapPolygon.getId(), point, mapPolygon.getStyle()))
            .toList());
    return builder.build();
  }
}
