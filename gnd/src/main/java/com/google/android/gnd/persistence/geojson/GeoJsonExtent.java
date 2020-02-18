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

import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import org.json.JSONArray;

class GeoJsonExtent {

  private final GeoJsonGeometry geometry;

  @Nullable
  GeoJsonExtent(GeoJsonGeometry geometry) {
    this.geometry = geometry;
  }

  ImmutableList<LatLng> getVertices() {
    if (geometry.getVertices().isEmpty()) {
      return ImmutableList.of();
    }

    JSONArray sw = geometry.getVertices().map(j -> j.optJSONArray(0)).orElse(null);
    JSONArray ne = geometry.getVertices().map(j -> j.optJSONArray(2)).orElse(null);

    if (sw == null || ne == null) {
      return ImmutableList.of();
    }

    double south = sw.optDouble(0, 0.0);
    double west = sw.optDouble(1, 0.0);
    double north = ne.optDouble(0, 0.0);
    double east = ne.optDouble(1, 0.0);

    return ImmutableList.of(
        new LatLng(south, west),
        new LatLng(north, east),
        new LatLng(south, east),
        new LatLng(north, west));
  }
}
