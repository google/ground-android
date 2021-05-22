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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java8.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;

/** Describes a tile set source, including its id, extents, and source URL. */
class TileSetSource {

  private static final String GEOMETRY_KEY = "geometry";
  private static final String VERTICES_JSON_KEY = "coordinates";

  private static final String ID_KEY = "id";
  private static final String PROPERTIES_KEY = "properties";
  private static final String URL_KEY = "url";

  private final JSONObject json;

  /**
   * Constructs a TileSetSource based on the contents of {@param jsonObject}.
   *
   * <p>A valid tile has the following information:
   *
   * <p>- a geometry describing a polygon. - an id specifying cartesian coordinates. - a URL
   * specifying a source for the tile imagery.
   *
   * <p>GeoJSON Polygons are described using coordinate arrays that form a linear ring. The first
   * and last value in a linear ring are equivalent. We assume coordinates are ordered, S/W, S/E,
   * N/E, N/W, (S/W again, closing the ring).
   *
   * <p>Interior rings, which describe holes in the polygon, are ignored.
   */
  TileSetSource(JSONObject jsonObject) {
    this.json = jsonObject;
  }

  private ImmutableList<LatLng> getVertices() {
    Optional<JSONArray> exteriorRing =
        Optional.ofNullable(json.optJSONObject(GEOMETRY_KEY))
            .flatMap(j -> Optional.ofNullable(j.optJSONArray(VERTICES_JSON_KEY)))
            .map(j -> j.optJSONArray(0));

    return ringCoordinatesToLatLngs(exteriorRing.orElse(null));
  }

  public Optional<String> getId() {
    String s = json.optString(ID_KEY);
    return s.isEmpty() ? Optional.empty() : Optional.of(s);
  }

  public Optional<String> getUrl() {
    return Optional.ofNullable(json.optJSONObject(PROPERTIES_KEY)).map(j -> j.optString(URL_KEY));
  }

  boolean boundsIntersect(LatLngBounds bounds) {
    return stream(this.getVertices()).anyMatch(bounds::contains);
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
