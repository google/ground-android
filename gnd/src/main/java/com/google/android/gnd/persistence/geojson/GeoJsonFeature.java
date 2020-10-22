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
import java8.util.Optional;
import org.json.JSONObject;

public class GeoJsonFeature {

  private static final String PROPERTIES_KEY = "properties";
  private static final String LETTER_KEY = "letter";
  private static final String COLOR_KEY = "color";
  private static final String RANK_KEY = "rank";
  private static final String ASCII_KEY = "ascii";

  // Geometry type
  private static final String POLYGON = "Polygon";

  private final JSONObject json;
  private final GeoJsonGeometry geometry;
  private final GeoJsonPolygon geoJsonPolygon;

  public GeoJsonFeature(JSONObject json) {
    this.json = json;
    geometry = new GeoJsonGeometry(json);
    geoJsonPolygon = new GeoJsonPolygon(geometry);
  }

  public ImmutableList<ImmutableList<LatLng>> getVertices() {
    return geoJsonPolygon.getAllVertices();
  }

  public boolean isPolygon() {
    return geometry.getType().map(s -> s.equals(POLYGON)).orElse(false);
  }

  private Optional<String> getProperty(String key) {
    return Optional.ofNullable(json.optJSONObject(PROPERTIES_KEY)).map(j -> j.optString(key));
  }

  public Optional<String> getLetter() {
    return getProperty(LETTER_KEY);
  }

  public String getColor() {
    return getProperty(COLOR_KEY).orElse("000000");
  }

  public Optional<String> getRank() {
    return getProperty(RANK_KEY);
  }

  public Optional<String> getAscii() {
    return getProperty(ASCII_KEY);
  }
}
