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

package com.google.android.gnd.util;

import static java8.util.J8Arrays.stream;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
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
import java8.util.Optional;
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

  static class Cartesian {
    private final int xcoordinate;
    private final int ycoordinate;
    private final int zcoordinate;

    Cartesian(int x, int y, int z) {
      this.xcoordinate = x;
      this.ycoordinate = y;
      this.zcoordinate = z;
    }

    private static Cartesian fromString(String catersianCoords) {
      String[] values = catersianCoords.replaceAll("[()]", "").split(",");
      int[] coords = new int[3];

      for (int i = 0; i < coords.length && i < values.length; i++) {
        coords[i] = Integer.parseInt(values[i]);
      }

      return new Cartesian(coords[0], coords[1], coords[2]);
    }
  }

  /**
   * A GeoJSONTile is any polygon that describes a single exterior ring comprised of four ordered
   * coordinates: South/West, South/East, North/West, North/East, has a cartesian representation,
   * and has an associated URL.
   */
  private static class GeoJsonTile {

    private final LatLng[] coordinates;
    private final Cartesian cartesian;
    private final String url;

    private GeoJsonTile(LatLng[] coordinates, Cartesian cartesian, String url) {
      this.coordinates = coordinates.clone();
      this.cartesian = cartesian;
      this.url = url;
    }

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
    private static Optional<GeoJsonTile> fromJsonObject(JSONObject jsonObject) {
      try {
        JSONObject geometry = jsonObject.getJSONObject("geometry");
        JSONArray sw = geometry.getJSONArray("coordinates").getJSONArray(0);
        JSONArray ne = geometry.getJSONArray("coordinates").getJSONArray(2);
        String id = jsonObject.getString("id");
        String url = jsonObject.getJSONObject("properties").getString("title");

        double south = sw.getDouble(0);
        double west = sw.getDouble(1);
        double north = ne.getDouble(0);
        double east = ne.getDouble(1);

        LatLng[] coords = {
          new LatLng(south, west),
          new LatLng(south, east),
          new LatLng(north, west),
          new LatLng(north, east)
        };

        Cartesian cartesian = Cartesian.fromString(id);

        return Optional.of(new GeoJsonTile(coords, cartesian, url));
      } catch (JSONException e) {
        Log.e(TAG, "failed to parse geoJSONPolygon", e);
      }
      return Optional.empty();
    }

    /**
     * Returns true iff any of {@link GeoJsonTile#coordinates} are contained within {@param bounds}.
     *
     * <p>This method assumes {@param bounds} is larger than the bounds described by {@link
     * GeoJsonTile#coordinates} and does not account for cases in which the inverse is true.
     */
    private boolean intersects(LatLngBounds bounds) {
      return stream(this.coordinates).anyMatch(bounds::contains);
    }
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

      stream(toArray(features))
          .filter(GeoJsonParser::hasGeometry)
          .filter(GeoJsonParser::isPolygon)
          .map(GeoJsonTile::fromJsonObject)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .filter(tile -> tile.intersects(bounds))
          .map(this::jsonToTile);

    } catch (IOException | JSONException e) {
      Log.e(TAG, "Unable to load JSON layer", e);
    }
    return ImmutableList.of();
  }

  /** Returns the {@link Tile} specified by {@param json}. */
  private Tile jsonToTile(GeoJsonTile json) {
    int x = json.cartesian.xcoordinate;
    int y = json.cartesian.ycoordinate;
    int z = json.cartesian.zcoordinate;

    return Tile.newBuilder()
        .setId(uuidGenerator.generateUuid())
        .setUrl(json.url)
        .setState(State.PENDING)
        .setX(x)
        .setY(y)
        .setZ(z)
        .setPath(Tile.pathFromCoords(x, y, z))
        .build();
  }

  /** Returns true if {@param geoJsonGeometry} describes a polygon. */
  private static boolean isPolygon(JSONObject geoJsonGeometry) {
    String type = geoJsonGeometry.optString("type");
    return "Polygon".equals(type);
  }

  /** Returns true if {@param geoJsonObject} is a geometry object. */
  private static boolean hasGeometry(JSONObject geoJsonObject) {
    JSONObject g = geoJsonObject.optJSONObject("geometry");
    return g != null;
  }
}
