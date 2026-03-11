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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Geometry
import org.groundplatform.domain.model.geometry.LineString
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.MultiPolygon
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.geometry.Polygon
import org.groundplatform.domain.model.locationofinterest.LoiProperties
import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.domain.model.locationofinterest.generateProperties

@Suppress("MultilineRawStringIndentation")
class GetLoiReportUseCaseTest {
  private val loiDataProvider = FakeLoiDataProvider()
  private val getLoiReportUseCase = GetLoiReportUseCase(loiDataProvider)

  @Test
  fun `Should get a report with the correct geoJson for a Point`() = runTest {
    val loiReport =
      invokeUseCase(
        geometry = Point(Coordinates(lat = 41.0, lng = -89.0)),
        properties = generateProperties("Point test"),
      )

    val expectedGeoJson =
      """
      {
        "type": "Feature",
        "properties": {"name": "Point test"},
        "geometry": {
          "type": "Point",
          "coordinates": [-89.0, 41.0]
        }
      }
      """
        .trimIndent()
    assertEquals(Json.parseToJsonElement(expectedGeoJson), loiReport.geoJson)
  }

  @Test
  fun `Should get a report with the correct geoJson for a Polygon`() = runTest {
    val shell =
      LinearRing(
        listOf(
          Coordinates(0.0, 0.0),
          Coordinates(1.0, 0.0),
          Coordinates(1.0, 1.0),
          Coordinates(0.0, 0.0),
        )
      )
    val loiReport =
      invokeUseCase(geometry = Polygon(shell), properties = generateProperties("Polygon test"))

    val expectedGeoJson =
      """
      {
        "type": "Feature",
        "properties": {"name": "Polygon test"},
        "geometry": {
          "type": "Polygon",
          "coordinates": [
            [[0.0, 0.0], [0.0, 1.0], [1.0, 1.0], [0.0, 0.0]]
          ]
        }
      }
      """
        .trimIndent()
    assertEquals(Json.parseToJsonElement(expectedGeoJson), loiReport.geoJson)
  }

  @Test
  fun `Should get a report with the correct geoJson for a Polygon with holes`() = runTest {
    val shell =
      LinearRing(
        listOf(
          Coordinates(0.0, 0.0),
          Coordinates(10.0, 0.0),
          Coordinates(10.0, 10.0),
          Coordinates(0.0, 0.0),
        )
      )
    val hole =
      LinearRing(
        listOf(
          Coordinates(2.0, 2.0),
          Coordinates(3.0, 2.0),
          Coordinates(3.0, 3.0),
          Coordinates(2.0, 2.0),
        )
      )
    val loiReport =
      invokeUseCase(
        geometry = Polygon(shell, listOf(hole)),
        properties = generateProperties("Polygon with holes test"),
      )

    val expectedGeoJson =
      """
      {
        "type": "Feature",
        "properties": {"name": "Polygon with holes test"},
        "geometry": {
          "type": "Polygon",
          "coordinates": [
            [[0.0, 0.0], [0.0, 10.0], [10.0, 10.0], [0.0, 0.0]],
            [[2.0, 2.0], [2.0, 3.0], [3.0, 3.0], [2.0, 2.0]]
          ]
        }
      }
      """
        .trimIndent()
    assertEquals(Json.parseToJsonElement(expectedGeoJson), loiReport.geoJson)
  }

  @Test
  fun `Should get a report with the correct geoJson for a MultiPolygon`() = runTest {
    val shell1 =
      LinearRing(
        listOf(
          Coordinates(0.0, 0.0),
          Coordinates(1.0, 0.0),
          Coordinates(1.0, 1.0),
          Coordinates(0.0, 0.0),
        )
      )
    val shell2 =
      LinearRing(
        listOf(
          Coordinates(5.0, 5.0),
          Coordinates(6.0, 5.0),
          Coordinates(6.0, 6.0),
          Coordinates(5.0, 5.0),
        )
      )
    val loiReport =
      invokeUseCase(
        geometry = MultiPolygon(listOf(Polygon(shell1), Polygon(shell2))),
        properties = generateProperties("MultiPolygon test"),
      )

    val expectedGeoJson =
      """
      {
        "type": "Feature",
        "properties": {"name": "MultiPolygon test"},
        "geometry": {
          "type": "MultiPolygon",
          "coordinates": [
            [[[0.0, 0.0], [0.0, 1.0], [1.0, 1.0], [0.0, 0.0]]],
            [[[5.0, 5.0], [5.0, 6.0], [6.0, 6.0], [5.0, 5.0]]]
          ]
        }
      }
      """
        .trimIndent()
    assertEquals(Json.parseToJsonElement(expectedGeoJson), loiReport.geoJson)
  }

  @Test
  fun `Should get a report with the correct geoJson for a LineString`() = runTest {
    val lineString =
      LineString(listOf(Coordinates(10.0, 20.0), Coordinates(30.0, 40.0), Coordinates(50.0, 60.0)))
    val loiReport =
      invokeUseCase(geometry = lineString, properties = generateProperties("LineString test"))

    val expectedGeoJson =
      """
      {
        "type": "Feature",
        "properties": {"name": "LineString test"},
        "geometry": {
          "type": "LineString",
          "coordinates": [[20.0, 10.0], [40.0, 30.0], [60.0, 50.0]]
        }
      }
      """
        .trimIndent()
    assertEquals(Json.parseToJsonElement(expectedGeoJson), loiReport.geoJson)
  }

  @Test
  fun `Should get a report with empty properties when no name is provided`() = runTest {
    val loiReport =
      invokeUseCase(
        geometry = Point(Coordinates(lat = 0.0, lng = 0.0)),
        properties = generateProperties(),
      )

    val expectedGeoJson =
      """
      {
        "type": "Feature",
        "properties": {},
        "geometry": {
          "type": "Point",
          "coordinates": [0.0, 0.0]
        }
      }
      """
        .trimIndent()
    assertEquals(Json.parseToJsonElement(expectedGeoJson), loiReport.geoJson)
  }

  @Test
  fun `Should throw error when geometry is a bare LinearRing`() = runTest {
    assertFailsWith<IllegalStateException> {
      invokeUseCase(
        geometry =
          LinearRing(
            listOf(
              Coordinates(lat = 0.0, lng = 0.0),
              Coordinates(lat = 1.0, lng = 0.0),
              Coordinates(lat = 1.0, lng = 1.0),
              Coordinates(lat = 0.0, lng = 0.0),
            )
          ),
        properties = generateProperties(),
      )
    }
  }

  private suspend fun invokeUseCase(geometry: Geometry, properties: LoiProperties): LoiReport {
    loiDataProvider.result = LoiDataProviderInterface.LoiData(geometry, properties)
    return getLoiReportUseCase.invoke("loiId", "surveyId")!!
  }

  private class FakeLoiDataProvider : LoiDataProviderInterface {
    var result: LoiDataProviderInterface.LoiData? = null

    override suspend fun get(surveyId: String, loiId: String): LoiDataProviderInterface.LoiData? =
      result
  }
}
