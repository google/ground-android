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
import static java8.util.J8Arrays.stream;

import android.util.Log;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.model.basemap.tile.Tile.State;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.inject.Inject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GeoJsonParser {

  private static final String TAG = GeoJsonParser.class.getSimpleName();
  private final OfflineUuidGenerator uuidGenerator;

  @Inject
  GeoJsonParser(OfflineUuidGenerator uuidGenerator) {
    this.uuidGenerator = uuidGenerator;
  }

  /**
   * Converts a JSONArray to an array of JSONObjects. Provided for compatibility with java8 streams.
   * JSONArray itself only inherits from Object, and is not convertible to a stream.
   */
  private static JSONObject[] toArray(JSONArray arr) {
    JSONObject[] result = new JSONObject[arr.length()];

    for (int i = 0; i < arr.length(); i++) {
      try {
        JSONObject o = arr.getJSONObject(i);
        result[i] = o;
      } catch (JSONException e) {
        Log.e(TAG, "couldn't parse json", e);
      }
    }

    return result;
  }

  /**
   * Returns the immutable list of tiles specified in {@param geojson} that intersect {@param
   * bounds}.
   */
  public ImmutableList<Tile> intersectingTiles(LatLngBounds bounds, File geojson) {
    try {
      InputStream is = new FileInputStream(geojson);
      BufferedReader buf = new BufferedReader(new InputStreamReader(is));
      String line = buf.readLine();
      StringBuilder sb = new StringBuilder();
      while (line != null) {
        sb.append(line).append('\n');
        line = buf.readLine();
      }

      JSONObject geoJson = new JSONObject(sb.toString());
      JSONArray features = geoJson.getJSONArray("features");

      return stream(toArray(features))
          .map(GeoJsonTile::new)
          .filter(tile -> tile.boundsIntersect(bounds))
          .map(this::jsonToTile)
          .collect(toImmutableList());

    } catch (IOException | JSONException e) {
      Log.e(TAG, "Unable to load JSON layer", e);
    }
    return ImmutableList.of();
  }

  /** Returns the {@link Tile} specified by {@param json}. */
  private Tile jsonToTile(GeoJsonTile json) {
    return Tile.newBuilder()
        .setId(uuidGenerator.generateUuid())
        .setUrl(json.getUrl().get())
        .setState(State.PENDING)
        .setPath(Tile.pathFromId(json.getId().get()))
        .build();
  }
}
