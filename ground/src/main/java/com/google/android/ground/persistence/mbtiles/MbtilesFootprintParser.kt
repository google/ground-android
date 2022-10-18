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
package com.google.android.ground.persistence.mbtiles

import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.model.basemap.tile.TileSet.Companion.pathFromId
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import io.reactivex.Single
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class MbtilesFootprintParser @Inject constructor(private val uuidGenerator: OfflineUuidGenerator) {

  private fun getJsonTileSets(jsonSource: File): Single<ImmutableList<TileSetJson>> =
    try {
      val fileContents =
        FileUtils.readFileToString(jsonSource, Charset.forName(JSON_SOURCE_CHARSET))
      val geoJson = JSONObject(fileContents)
      val locationsOfInterest = geoJson.getJSONArray(LOCATIONS_OF_INTEREST_KEY)
      Single.just(
        toArrayList(locationsOfInterest)
          .map { jsonObject: JSONObject -> TileSetJson(jsonObject) }
          .toImmutableList()
      )
    } catch (e: Exception) {
      Single.error(e)
    }

  fun allTiles(file: File): Single<ImmutableList<TileSet>> =
    getJsonTileSets(file)
      .map { tilesetJsonList -> tilesetJsonList.map { jsonToTileSet(it) }.toImmutableList() }
      .doOnError { Timber.e(it) }

  /**
   * Returns the immutable list of tiles specified in {@param geojson} that intersect {@param
   * bounds}.
   */
  fun intersectingTiles(bounds: LatLngBounds, file: File): Single<ImmutableList<TileSet>> {
    return getJsonTileSets(file)
      .map { tilesetJsonList ->
        tilesetJsonList
          .filter { it.boundsIntersect(bounds) }
          .map { jsonToTileSet(it).incrementOfflineAreaCount() }
          .toImmutableList()
      }
      .doOnError { Timber.e(it) }
  }

  // TODO: Instead of returning tiles with invalid state (empty URL/ID values), throw an exception
  //  here and handle it downstream.
  /** Returns the [TileSet] specified by {@param json}. */
  private fun jsonToTileSet(json: TileSetJson): TileSet =
    TileSet(
      json.url.orElse(""),
      uuidGenerator.generateUuid(),
      pathFromId(json.id.orElse("")),
      TileSet.State.PENDING,
      0
    )

  companion object {
    // TODO: s/features/locations_of_interest key once we have changed the MBtiles schema (if this
    //  even sticks around).
    private const val LOCATIONS_OF_INTEREST_KEY = "features"
    private const val JSON_SOURCE_CHARSET = "UTF-8"

    /**
     * Converts a JSONArray to an array of JSONObjects. Provided for compatibility with java8
     * streams. JSONArray itself only inherits from Object, and is not convertible to a stream.
     */
    private fun toArrayList(arr: JSONArray): List<JSONObject> {
      val result: MutableList<JSONObject> = ArrayList()
      for (i in 0 until arr.length()) {
        try {
          result.add(arr.getJSONObject(i))
        } catch (e: JSONException) {
          Timber.e(e, "Ignoring error in JSON array")
        }
      }
      return result
    }
  }
}
