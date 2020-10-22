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

package com.google.android.gnd.persistence.geojson;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java8.util.Optional;
import javax.annotation.Nullable;
import org.json.JSONArray;

class GeoJsonPolygon {

  private final GeoJsonGeometry geometry;

  @Nullable
  GeoJsonPolygon(GeoJsonGeometry geometry) {
    this.geometry = geometry;
  }

  ImmutableList<ImmutableList<LatLng>> getAllVertices() {
    return geometry
        .getVertices()
        .map(
            jsonArray -> {
              List<ImmutableList<LatLng>> listVertices = new ArrayList<>();
              for (int i = 0; i < jsonArray.length(); i++) {
                JSONArray vertices = jsonArray.optJSONArray(i);
                listVertices.add(ringCoordinatesToLatLngs(vertices));
              }
              return ImmutableList.copyOf(listVertices);
            })
        .orElse(ImmutableList.of());
  }

  ImmutableList<LatLng> getVertices() {
    Optional<JSONArray> exteriorRing = geometry.getVertices().map(j -> j.optJSONArray(0));

    return ringCoordinatesToLatLngs(exteriorRing.orElse(null));
  }

  private ImmutableList<LatLng> ringCoordinatesToLatLngs(JSONArray exteriorRing) {
    if (exteriorRing == null) {
      return ImmutableList.of();
    }

    List<LatLng> coordinates = new ArrayList<>();

    for (int i = 0; i < exteriorRing.length(); i++) {
      JSONArray point = exteriorRing.optJSONArray(i);
      double lat = point.optDouble(1, 0.0);
      double lng = point.optDouble(0, 0.0);

      // PMD complains about instantiating objects in loops, but here, we retain a reference to the
      // object after the loop exits--the PMD recommendation here makes little sense, and is
      // presumably intended to prevent short-lived allocations.
      coordinates.add(new LatLng(lat, lng)); // NOPMD
    }

    return stream(coordinates).collect(toImmutableList());
  }
}
