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

import static java8.util.stream.StreamSupport.stream;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import org.json.JSONObject;

/**
 * A GeoJSONTile is any polygon that describes a single exterior ring comprised of four ordered
 * coordinates: South/West, South/East, North/West, North/East, has a cartesian representation, and
 * has an associated URL.
 */
public class GeoJsonTile {

  private static final String ID_KEY = "id";
  private static final String PROPERTIES_KEY = "properties";
  private static final String URL_KEY = "url";

  private final JSONObject json;

  /**
   * Constructs a GeoJSONTile based on the contents of {@param jsonObject}.
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
  GeoJsonTile(JSONObject jsonObject) {
    this.json = jsonObject;
  }

  private ImmutableList<LatLng> getVertices() {
    return new GeoJsonPolygon(new GeoJsonGeometry(json)).getVertices();
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
}
