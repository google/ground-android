/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.domain.usecases

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Geometry
import org.groundplatform.domain.model.geometry.LineString
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.MultiPolygon
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.geometry.Polygon
import org.groundplatform.domain.model.locationofinterest.LoiProperties
import org.groundplatform.domain.model.locationofinterest.LoiReport

class GetLoiReportUseCase(private val loiGeometryProvider: LoiDataProviderInterface) {
  suspend operator fun invoke(loiId: String, surveyId: String): LoiReport? {
    val loiData = loiGeometryProvider.get(surveyId, loiId)
    return loiData?.let { LoiReport(it.geometry.toGeoJson(it.properties)) }
  }

  /**
   * Converts a [Geometry] to its GeoJSON representation as defined by
   * [RFC 7946](https://datatracker.ietf.org/doc/html/rfc7946).
   */
  private fun Geometry.toGeoJson(loiProperties: LoiProperties): JsonObject {
    val geometryJson =
      when (this) {
        is Point -> geoJsonObject("Point", coordinatesToPosition(coordinates))
        is LineString -> geoJsonObject("LineString", coordinatesToPositions(coordinates))
        is LinearRing -> geoJsonObject("LineString", coordinatesToPositions(coordinates))
        is Polygon -> geoJsonObject("Polygon", polygonToCoordinates(this))
        is MultiPolygon ->
          geoJsonObject("MultiPolygon", JsonArray(polygons.map { polygonToCoordinates(it) }))
      }
    return JsonObject(
      mapOf(
        "type" to JsonPrimitive("Feature"),
        "properties" to JsonObject(loiProperties.mapValues { JsonPrimitive(it.value.toString()) }),
        "geometry" to geometryJson,
      )
    )
  }

  private fun geoJsonObject(type: String, coordinates: JsonElement): JsonObject =
    JsonObject(mapOf("type" to JsonPrimitive(type), "coordinates" to coordinates))

  /** Converts a single [Coordinates] to a GeoJSON position: `[lng, lat]`. */
  private fun coordinatesToPosition(coordinates: Coordinates): JsonArray =
    JsonArray(listOf(JsonPrimitive(coordinates.lng), JsonPrimitive(coordinates.lat)))

  /** Converts a list of [Coordinates] to a GeoJSON array of positions. */
  private fun coordinatesToPositions(coordinates: List<Coordinates>): JsonArray =
    JsonArray(coordinates.map { coordinatesToPosition(it) })

  /** Converts a [Polygon] to its GeoJSON coordinates (shell + holes as ring arrays). */
  private fun polygonToCoordinates(polygon: Polygon): JsonArray {
    val rings = mutableListOf(coordinatesToPositions(polygon.shell.coordinates))
    polygon.holes.forEach { rings.add(coordinatesToPositions(it.coordinates)) }
    return JsonArray(rings)
  }
}
