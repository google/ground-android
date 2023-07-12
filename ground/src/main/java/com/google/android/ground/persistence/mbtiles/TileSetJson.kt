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

import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.gms.GmsExt.contains
import org.json.JSONArray
import org.json.JSONObject

/**
 * Describes a tile set source, including its id, extents, and source URL.
 *
 * A valid tile has the following information:
 * - a geometry describing a polygon.
 * - an id specifying cartesian coordinates.
 * - a URL specifying a source for the tile imagery.
 *
 * GeoJSON Polygons are described using coordinate arrays that task a linear ring. The first and
 * last value in a linear ring are equivalent. We assume coordinates are ordered, S/W, S/E, N/E,
 * N/W, (S/W again, closing the ring).
 *
 * Interior rings, which describe holes in the polygon, are ignored.
 */
internal class TileSetJson(private val json: JSONObject) {

  private val vertices: List<Coordinate>
    get() {
      val exteriorRing =
        json.optJSONObject(GEOMETRY_KEY)?.optJSONArray(VERTICES_JSON_KEY)?.optJSONArray(0)
      return jsonArrayToCoordinates(exteriorRing)
    }

  val id: String = json.optString(ID_KEY) ?: ""

  val url: String = json.optJSONObject(PROPERTIES_KEY)?.optString(URL_KEY) ?: ""

  fun boundsIntersect(bounds: Bounds): Boolean = vertices.any { bounds.contains(it) }

  private fun jsonArrayToCoordinates(exteriorRing: JSONArray?): List<Coordinate> {
    if (exteriorRing == null) {
      return listOf()
    }
    val coordinates: MutableList<Coordinate> = ArrayList()
    for (i in 0 until exteriorRing.length()) {
      val point = exteriorRing.optJSONArray(i)
      val lat = point.optDouble(1, 0.0)
      val lng = point.optDouble(0, 0.0)
      coordinates.add(Coordinate(lat, lng))
    }
    return coordinates
  }

  companion object {
    private const val GEOMETRY_KEY = "geometry"
    private const val VERTICES_JSON_KEY = "coordinates"
    private const val ID_KEY = "id"
    private const val PROPERTIES_KEY = "properties"
    private const val URL_KEY = "url"
  }
}
