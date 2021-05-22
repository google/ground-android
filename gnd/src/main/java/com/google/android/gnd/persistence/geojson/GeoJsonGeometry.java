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

import java8.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;

class GeoJsonGeometry {

  private static final String GEOMETRY_KEY = "geometry";
  private static final String GEOMETRY_TYPE_KEY = "type";
  private static final String VERTICES_JSON_KEY = "coordinates";

  private final JSONObject json;

  GeoJsonGeometry(JSONObject jsonObject) {
    this.json = jsonObject;
  }

  private Optional<JSONObject> getJson() {
    return Optional.ofNullable(json.optJSONObject(GEOMETRY_KEY));
  }

  public Optional<String> getType() {
    return getJson().map(j -> j.optString(GEOMETRY_TYPE_KEY));
  }

  Optional<JSONArray> getVertices() {
    return getJson().flatMap(j -> Optional.ofNullable(j.optJSONArray(VERTICES_JSON_KEY)));
  }
}
