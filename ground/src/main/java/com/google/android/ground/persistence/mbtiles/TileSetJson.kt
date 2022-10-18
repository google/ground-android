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

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import java8.util.Optional
import org.json.JSONArray
import org.json.JSONObject

/**
 * Describes a tile set source, including its id, extents, and source URL.
 *
 * A valid tile has the following information:
 *
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

  private val vertices: ImmutableList<LatLng>
    get() {
      val exteriorRing =
        Optional.ofNullable(json.optJSONObject(GEOMETRY_KEY))
          .flatMap { j: JSONObject -> Optional.ofNullable(j.optJSONArray(VERTICES_JSON_KEY)) }
          .map { j: JSONArray -> j.optJSONArray(0) }
      return ringCoordinatesToLatLngs(exteriorRing.orElse(null))
    }

  val id: Optional<String>
    get() {
      val value = json.optString(ID_KEY)
      return if (value.isEmpty()) Optional.empty() else Optional.of(value)
    }

  val url: Optional<String>
    get() = Optional.ofNullable(json.optJSONObject(PROPERTIES_KEY)).map { it.optString(URL_KEY) }

  fun boundsIntersect(bounds: LatLngBounds): Boolean = vertices.any { bounds.contains(it) }

  private fun ringCoordinatesToLatLngs(exteriorRing: JSONArray?): ImmutableList<LatLng> {
    if (exteriorRing == null) {
      return ImmutableList.of()
    }
    val coordinates: MutableList<LatLng> = ArrayList()
    for (i in 0 until exteriorRing.length()) {
      val point = exteriorRing.optJSONArray(i)
      val lat = point.optDouble(1, 0.0)
      val lng = point.optDouble(0, 0.0)
      coordinates.add(LatLng(lat, lng))
    }
    return coordinates.toImmutableList()
  }

  companion object {
    private const val GEOMETRY_KEY = "geometry"
    private const val VERTICES_JSON_KEY = "coordinates"
    private const val ID_KEY = "id"
    private const val PROPERTIES_KEY = "properties"
    private const val URL_KEY = "url"
  }
}
