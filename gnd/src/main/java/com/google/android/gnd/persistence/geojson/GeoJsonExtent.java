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
import com.google.android.gms.maps.model.LatLngBounds;
import java8.util.Optional;
import javax.annotation.Nullable;
import org.json.JSONArray;

public class GeoJsonExtent {

  private final GeoJsonGeometry geometry;
  private final Optional<LatLng[]> coordinates;

  @Nullable
  GeoJsonExtent(GeoJsonGeometry geometry) {
    this.geometry = geometry;
    this.coordinates = getPolygonCoordinates();
  }

  Optional<LatLngBounds> getBounds() {
    return this.coordinates.map(cs -> new LatLngBounds(cs[0], cs[2]));
  }

  private Optional<LatLng[]> getPolygonCoordinates() {
    JSONArray sw = this.geometry.getCoordinates().map(j -> j.optJSONArray(0)).get();
    JSONArray ne = this.geometry.getCoordinates().map(j -> j.optJSONArray(2)).get();

    if (sw == null || ne == null) {
      return Optional.empty();
    }

    double south = sw.optDouble(0, 0.0);
    double west = sw.optDouble(1, 0.0);
    double north = ne.optDouble(0, 0.0);
    double east = ne.optDouble(1, 0.0);

    return Optional.of(
        new LatLng[] {
          new LatLng(south, west),
          new LatLng(south, east),
          new LatLng(north, east),
          new LatLng(north, west)
        });
  }

  public Optional<LatLng[]> getCoordinates() {
    return this.coordinates;
  }
}
