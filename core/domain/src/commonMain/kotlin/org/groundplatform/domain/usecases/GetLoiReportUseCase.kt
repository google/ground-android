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
import kotlinx.serialization.json.JsonUnquotedLiteral
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Geometry
import org.groundplatform.domain.model.geometry.LineString
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.MultiPolygon
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.geometry.Polygon
import org.groundplatform.domain.model.locationofinterest.LOI_ID_PROPERTY
import org.groundplatform.domain.model.locationofinterest.LOI_NAME_PROPERTY
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.SubmissionRepositoryInterface
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.groundplatform.domain.util.toFixedDecimals

/**
 * Use case that generates a [LoiReport] containing the LOI geometry and properties as a GeoJSON.
 *
 * Supported geometry types: [Point], [LineString], [Polygon], [MultiPolygon].
 */
class GetLoiReportUseCase(
  private val locationOfInterestRepository: LocationOfInterestRepositoryInterface,
  private val userRepositoryInterface: UserRepositoryInterface,
  private val surveyRepositoryInterface: SurveyRepositoryInterface,
  private val submissionRepositoryInterface: SubmissionRepositoryInterface,
  private val formatDateTime: (timestampMillis: Long, pattern: String) -> String,
) {
  /**
   * Returns a [LoiReport] for the given LOI, or `null` if it does not exist.
   *
   * @param loiName the name of the location of interest
   * @param loiId the identifier of the location of interest.
   * @param surveyId the identifier of the survey the LOI belongs to.
   * @throws IllegalStateException if the LOI geometry is a bare [LinearRing].
   */
  suspend operator fun invoke(loiName: String, loiId: String, surveyId: String): LoiReport? {
    val loi = locationOfInterestRepository.getOfflineLoi(surveyId, loiId) ?: return null
    val submissions =
      submissionRepositoryInterface.getSubmissions(loi).sortedByDescending {
        it.lastModified.clientTimestamp
      }
    val surveyName = surveyRepositoryInterface.getOfflineSurvey(surveyId)?.title.orEmpty()
    val submissionDetails =
      if (submissions.isNotEmpty()) {
        val user = userRepositoryInterface.getAuthenticatedUser()
        LoiReport.SubmissionDetails(
          surveyName = surveyName,
          userName = user.displayName,
          userEmail = user.email,
          submissions = submissions,
        )
      } else null

    return LoiReport(
      loiName = loiName,
      geoJson = loi.geometry.toGeoJson(loi = loi, surveyName = surveyName),
      submissionDetails = submissionDetails,
    )
  }

  /**
   * Converts a [Geometry] to its GeoJSON representation as defined by
   * [RFC 7946](https://datatracker.ietf.org/doc/html/rfc7946).
   */
  private fun Geometry.toGeoJson(loi: LocationOfInterest, surveyName: String): JsonObject {
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
    val properties = getLoiPropertiesMap(loi, surveyName)

    return JsonObject(
      mapOf(
        KEY_TYPE to JsonPrimitive(TYPE_FEATURE),
        KEY_PROPERTIES to JsonObject(properties),
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
    JsonArray(listOf(coordinates.lng.roundTo6Decimals(), coordinates.lat.roundTo6Decimals()))

  /** Converts a list of [Coordinates] to a GeoJSON array of positions. */
  private fun coordinatesToPositions(coordinates: List<Coordinates>): JsonArray =
    JsonArray(coordinates.map { coordinatesToPosition(it) })

  /** Converts a [Polygon] to its GeoJSON coordinates (shell + holes as ring arrays). */
  private fun polygonToCoordinates(polygon: Polygon): JsonArray {
    val rings = mutableListOf(coordinatesToPositions(polygon.shell.coordinates))
    polygon.holes.forEach { rings.add(coordinatesToPositions(it.coordinates)) }
    return JsonArray(rings)
  }

  /** Renders this [Double] as a JSON number with exactly 6 decimal digits. */
  private fun Double.roundTo6Decimals(): JsonPrimitive {
    val value = toFixedDecimals(DECIMAL_DIGITS)
    return JsonUnquotedLiteral(value)
  }

  private fun getLoiPropertiesMap(
    loi: LocationOfInterest,
    surveyName: String,
  ): Map<String, JsonPrimitive> = buildMap {
    loi.properties[LOI_NAME_PROPERTY]?.let { put(LOI_NAME_PROPERTY, it.toJsonPrimitive()) }
      ?: loi.properties[LOI_ID_PROPERTY]?.let { put(LOI_ID_PROPERTY, it.toJsonPrimitive()) }
    put(KEY_SURVEY, JsonPrimitive(surveyName))
    if (loi.isPredefined != true) {
      put(
        KEY_DATE,
        JsonPrimitive(formatDateTime(loi.lastModified.clientTimestamp, QR_DATE_PATTERN)),
      )
    }
  }

  private companion object {
    const val KEY_TYPE = "type"
    const val TYPE_FEATURE = "Feature"
    const val KEY_PROPERTIES = "properties"
    const val KEY_GEOMETRY = "geometry"
    const val KEY_COORDINATES = "coordinates"
    const val KEY_SURVEY = "survey"
    const val KEY_DATE = "date"
    const val TYPE_POINT = "Point"
    const val TYPE_LINE_STRING = "LineString"
    const val TYPE_POLYGON = "Polygon"
    const val TYPE_MULTI_POLYGON = "MultiPolygon"
    const val DECIMAL_DIGITS = 6
    const val QR_DATE_PATTERN = "yyyyMMdd HH:mm"
  }
}
