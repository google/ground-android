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

package org.groundplatform.domain.model.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeometryTest {

  private val c1 = Coordinates(0.0, 0.0)
  private val c2 = Coordinates(0.0, 1.0)
  private val c3 = Coordinates(1.0, 1.0)
  private val c4 = Coordinates(1.0, 0.0)

  @Test
  fun point_properties() {
    val point = Point(c1)
    assertFalse(point.isEmpty())
    assertEquals(c1, point.center())
    assertEquals(0.0, point.area())
    assertEquals(listOf(c1), point.getShellCoordinates())
  }

  @Test
  fun lineString_properties() {
    val lineString = LineString.lineStringOf(c1, c2, c3)
    assertFalse(lineString.isEmpty())
    assertEquals(0.0, lineString.area())
    assertEquals(listOf(c1, c2, c3), lineString.getShellCoordinates())
    assertFalse(lineString.isClosed())
  }

  @Test
  fun linearRing_and_polygon_area() {
    val shell = LinearRing(listOf(c1, c2, c3, c4, c1))
    assertTrue(shell.area() > 0.0)

    val polygon = Polygon(shell)
    assertEquals(shell.area(), polygon.area())

    val hole =
      LinearRing(
        listOf(
          Coordinates(0.2, 0.2),
          Coordinates(0.2, 0.8),
          Coordinates(0.8, 0.8),
          Coordinates(0.8, 0.2),
          Coordinates(0.2, 0.2),
        )
      )
    val polygonWithHole = Polygon(shell, listOf(hole))
    assertTrue(polygonWithHole.area() < polygon.area())
    assertEquals(shell.area() - hole.area(), polygonWithHole.area(), 1e-6)

    val multiPolygon = MultiPolygon(listOf(polygon, polygonWithHole))
    assertEquals(polygon.area() + polygonWithHole.area(), multiPolygon.area(), 1e-6)
  }

  @Test
  fun lineString_empty_properties() {
    val emptyLineString = LineString(emptyList())
    assertTrue(emptyLineString.isEmpty())
    assertEquals(emptyList(), emptyLineString.getShellCoordinates())
    assertEquals(0.0, emptyLineString.area())
  }

  @Test
  fun linearRing_empty_properties() {
    val emptyRing = LinearRing(emptyList())
    assertTrue(emptyRing.isEmpty())
    assertEquals(emptyList(), emptyRing.getShellCoordinates())
    assertEquals(0.0, emptyRing.area())
  }

  @Test
  fun polygon_getShellCoordinates_excludesHoles() {
    val shell = LinearRing(listOf(c1, c2, c3, c4, c1))
    val hole =
      LinearRing(
        listOf(
          Coordinates(0.2, 0.2),
          Coordinates(0.2, 0.8),
          Coordinates(0.8, 0.8),
          Coordinates(0.8, 0.2),
          Coordinates(0.2, 0.2),
        )
      )
    val polygon = Polygon(shell, listOf(hole))

    assertEquals(shell.coordinates, polygon.getShellCoordinates())
    assertFalse(polygon.getShellCoordinates().contains(Coordinates(0.2, 0.2)))
  }

  @Test
  fun polygon_area_holeExceedingShellArea_clampsToZero() {
    val shell = LinearRing(listOf(c1, c2, c3, c4, c1))
    val identicalHole = LinearRing(listOf(c1, c2, c3, c4, c1))
    val polygonZeroArea = Polygon(shell, listOf(identicalHole))
    assertEquals(0.0, polygonZeroArea.area())

    val polygonClamped = Polygon(shell, listOf(identicalHole, identicalHole))
    assertEquals(0.0, polygonClamped.area())
  }

  @Test
  fun multiPolygon_empty_properties() {
    val emptyMultiPolygon = MultiPolygon(emptyList())
    assertTrue(emptyMultiPolygon.isEmpty())
    assertEquals(emptyList(), emptyMultiPolygon.getShellCoordinates())
    assertEquals(0.0, emptyMultiPolygon.area())
  }

  @Test
  fun multiPolygon_getShellCoordinates_flattensMultiplePolygons() {
    val shell1 = LinearRing(listOf(c1, c2, c1))
    val shell2 = LinearRing(listOf(c3, c4, c3))
    val p1 = Polygon(shell1)
    val p2 = Polygon(shell2)
    val multiPolygon = MultiPolygon(listOf(p1, p2))

    assertEquals(shell1.coordinates + shell2.coordinates, multiPolygon.getShellCoordinates())
  }
}
