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

package com.google.android.gnd.persistence.mbtiles;

import static com.google.android.gnd.util.ImmutableListCollector.filterAndRecollect;
import static com.google.android.gnd.util.ImmutableListCollector.mapAndRecollect;
import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.model.basemap.tile.TileSet;
import com.google.android.gnd.model.basemap.tile.TileSet.State;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.common.collect.ImmutableList;
import io.reactivex.Single;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

public class MbtilesFootprintParser {

  private static final String FEATURES_KEY = "features";
  private static final String JSON_SOURCE_CHARSET = "UTF-8";

  private final OfflineUuidGenerator uuidGenerator;

  @Inject
  MbtilesFootprintParser(OfflineUuidGenerator uuidGenerator) {
    this.uuidGenerator = uuidGenerator;
  }

  /**
   * Converts a JSONArray to an array of JSONObjects. Provided for compatibility with java8 streams.
   * JSONArray itself only inherits from Object, and is not convertible to a stream.
   */
  private static List<JSONObject> toArrayList(JSONArray arr) {
    List<JSONObject> result = new ArrayList<>();

    for (int i = 0; i < arr.length(); i++) {
      try {
        result.add(arr.getJSONObject(i));
      } catch (JSONException e) {
        Timber.e(e, "Ignoring error in JSON array");
      }
    }

    return result;
  }

  private Single<ImmutableList<TileSetJson>> getJsonTileSets(File jsonSource) {
    try {
      String fileContents =
          FileUtils.readFileToString(jsonSource, Charset.forName(JSON_SOURCE_CHARSET));
      JSONObject geoJson = new JSONObject(fileContents);
      JSONArray features = geoJson.getJSONArray(FEATURES_KEY);

      ImmutableList<TileSetJson> tilesets =
          stream(toArrayList(features)).map(TileSetJson::new).collect(toImmutableList());

      return Single.just(tilesets);
    } catch (Exception e) {
      return Single.error(e);
    }
  }

  public Single<ImmutableList<TileSet>> allTiles(File file) {
    return getJsonTileSets(file).map(mapAndRecollect(this::jsonToTileSet)).doOnError(Timber::e);
  }

  /**
   * Returns the immutable list of tiles specified in {@param geojson} that intersect {@param
   * bounds}.
   */
  public Single<ImmutableList<TileSet>> intersectingTiles(LatLngBounds bounds, File file) {
    return getJsonTileSets(file)
        .map(filterAndRecollect(tile -> tile.boundsIntersect(bounds)))
        .map(mapAndRecollect(tileSetJson -> jsonToTileSet(tileSetJson).incrementOfflineAreaCount()))
        .doOnError(Timber::e);
  }

  /** Returns the {@link TileSet} specified by {@param json}. */
  private TileSet jsonToTileSet(TileSetJson json) {
    // TODO: Instead of returning tiles with invalid state (empty URL/ID values)
    // Throw an exception here and handle it downstream.
    return TileSet.newBuilder()
        .setId(uuidGenerator.generateUuid())
        .setUrl(json.getUrl().orElse(""))
        .setState(State.PENDING)
        .setPath(TileSet.pathFromId(json.getId().orElse("")))
        .setOfflineAreaReferenceCount(0)
        .build();
  }
}
