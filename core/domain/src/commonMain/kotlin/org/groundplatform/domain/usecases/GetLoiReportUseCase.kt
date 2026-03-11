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

/**
 * Use case that generates a [LoiReport] containing the LOI geometry and properties as a GeoJSON.
 *
 * Supported geometry types: [Point], [LineString], [Polygon], [MultiPolygon].
 */
class GetLoiReportUseCase(private val loiGeometryProvider: LoiDataProviderInterface) {
  /**
   * Returns a [LoiReport] for the given LOI, or `null` if it does not exist.
   *
   * @param loiId the identifier of the location of interest.
   * @param surveyId the identifier of the survey the LOI belongs to.
   * @throws IllegalStateException if the LOI geometry is a bare [LinearRing].
   */
  suspend operator fun invoke(loiName: String, loiId: String, surveyId: String): LoiReport? {
    val loiData = loiGeometryProvider.get(surveyId, loiId)
    return loiData?.let { LoiReport(loiName, it.geometry.toGeoJson(it.properties)) }
  }

  /**
   * Converts a [Geometry] to its GeoJSON representation as defined by
   * [RFC 7946](https://datatracker.ietf.org/doc/html/rfc7946).
   */
  private fun Geometry.toGeoJson(loiProperties: LoiProperties): JsonObject {
    val geometryJson =
      when (this) {
        is Point -> geoJsonObject(TYPE_POINT, coordinatesToPosition(coordinates))
        is LineString -> geoJsonObject(TYPE_LINE_STRING, coordinatesToPositions(coordinates))
        is LinearRing ->
          error(
            "LinearRing cannot be exported as GeoJSON. They are only used inside Polygon coordinates."
          )
        is Polygon -> geoJsonObject(TYPE_POLYGON, polygonToCoordinates(this))
        is MultiPolygon ->
          geoJsonObject(TYPE_MULTI_POLYGON, JsonArray(polygons.map { polygonToCoordinates(it) }))
      }
    return JsonObject(
      mapOf(
        KEY_TYPE to JsonPrimitive(TYPE_FEATURE),
        KEY_PROPERTIES to JsonObject(loiProperties.mapValues { it.value.toJsonPrimitive() }),
        KEY_GEOMETRY to geometryJson,
      )
    )
  }

  private fun Any.toJsonPrimitive(): JsonPrimitive =
    when (this) {
      is String -> JsonPrimitive(this)
      is Number -> JsonPrimitive(this)
      is Boolean -> JsonPrimitive(this)
      else -> JsonPrimitive(this.toString())
    }

  private fun geoJsonObject(type: String, coordinates: JsonElement): JsonObject =
    JsonObject(mapOf(KEY_TYPE to JsonPrimitive(type), KEY_COORDINATES to coordinates))

  /** Converts a single [Coordinates] to a GeoJSON position: [lng, lat]. */
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

  private companion object {
    const val KEY_TYPE = "type"
    const val TYPE_FEATURE = "Feature"
    const val KEY_PROPERTIES = "properties"
    const val KEY_GEOMETRY = "geometry"
    const val KEY_COORDINATES = "coordinates"
    const val TYPE_POINT = "Point"
    const val TYPE_LINE_STRING = "LineString"
    const val TYPE_POLYGON = "Polygon"
    const val TYPE_MULTI_POLYGON = "MultiPolygon"
  }
}
