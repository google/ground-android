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

package org.groundplatform.domain.model.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.LineString
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.MultiPolygon
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.geometry.Polygon

class BoundsTest {

  private val bounds = Bounds(south = 10.0, west = 20.0, north = 30.0, east = 40.0)

  @Test
  fun contains_coordinates_insideReturnsTrue() {
    assertTrue(bounds.contains(Coordinates(20.0, 30.0)))
  }

  @Test
  fun contains_coordinates_onBoundaryReturnsTrue() {
    assertTrue(bounds.contains(Coordinates(10.0, 20.0)))
    assertTrue(bounds.contains(Coordinates(30.0, 40.0)))
  }

  @Test
  fun contains_coordinates_outsideReturnsFalse() {
    assertFalse(bounds.contains(Coordinates(5.0, 30.0)))
    assertFalse(bounds.contains(Coordinates(35.0, 30.0)))
    assertFalse(bounds.contains(Coordinates(20.0, 15.0)))
    assertFalse(bounds.contains(Coordinates(20.0, 45.0)))
  }

  @Test
  fun contains_coordinates_antiMeridianCrossing() {
    val antiMeridianBounds = Bounds(south = -10.0, west = 170.0, north = 10.0, east = -170.0)

    assertTrue(antiMeridianBounds.contains(Coordinates(0.0, 175.0)))
    assertTrue(antiMeridianBounds.contains(Coordinates(0.0, -175.0)))
    assertFalse(antiMeridianBounds.contains(Coordinates(0.0, 0.0)))
  }

  @Test
  fun contains_pointGeometry() {
    assertTrue(bounds.contains(Point(Coordinates(20.0, 30.0))))
    assertFalse(bounds.contains(Point(Coordinates(0.0, 0.0))))
  }

  @Test
  fun contains_lineStringGeometry() {
    val insideLine = LineString.lineStringOf(Coordinates(20.0, 30.0), Coordinates(25.0, 35.0))
    val outsideLine = LineString.lineStringOf(Coordinates(0.0, 0.0), Coordinates(5.0, 5.0))
    val intersectingLine = LineString.lineStringOf(Coordinates(0.0, 0.0), Coordinates(20.0, 30.0))

    assertTrue(bounds.contains(insideLine))
    assertFalse(bounds.contains(outsideLine))
    assertTrue(bounds.contains(intersectingLine))
  }

  @Test
  fun contains_polygonGeometry() {
    val shellInside =
      LinearRing(
        listOf(
          Coordinates(15.0, 25.0),
          Coordinates(25.0, 25.0),
          Coordinates(25.0, 35.0),
          Coordinates(15.0, 25.0),
        )
      )
    val shellOutside =
      LinearRing(
        listOf(
          Coordinates(0.0, 0.0),
          Coordinates(5.0, 0.0),
          Coordinates(5.0, 5.0),
          Coordinates(0.0, 0.0),
        )
      )

    assertTrue(bounds.contains(Polygon(shellInside)))
    assertFalse(bounds.contains(Polygon(shellOutside)))
  }

  @Test
  fun getShellCoordinates_returnsExpectedCoordinates() {
    val c1 = Coordinates(1.0, 2.0)
    val c2 = Coordinates(3.0, 4.0)
    val c3 = Coordinates(1.0, 2.0)

    val point = Point(c1)
    assertEquals(listOf(c1), point.getShellCoordinates())

    val lineString = LineString.lineStringOf(c1, c2)
    assertEquals(listOf(c1, c2), lineString.getShellCoordinates())

    val linearRing = LinearRing(listOf(c1, c2, c3))
    assertEquals(listOf(c1, c2, c3), linearRing.getShellCoordinates())

    val polygon = Polygon(linearRing)
    assertEquals(listOf(c1, c2, c3), polygon.getShellCoordinates())

    val multiPolygon = MultiPolygon(listOf(polygon))
    assertEquals(listOf(c1, c2, c3), multiPolygon.getShellCoordinates())
  }

  @Test
  fun center_standardBounds() {
    assertEquals(Coordinates(20.0, 30.0), bounds.center)
    assertEquals(Coordinates(20.0, 30.0), bounds.center())
  }

  @Test
  fun center_antiMeridianCrossingBounds() {
    val antiMeridianBounds = Bounds(south = -10.0, west = 170.0, north = 10.0, east = -170.0)
    assertEquals(0.0, antiMeridianBounds.center.lat)
    val lng = antiMeridianBounds.center.lng
    assertTrue(lng == 180.0 || lng == -180.0)
  }

  @Test
  fun fromCoordinates_emptyCoordinates_returnsNull() {
    assertEquals(null, Bounds.fromCoordinates(emptyList()))
    assertEquals(null, Bounds.fromGeometries(emptyList()))
  }

  @Test
  fun fromCoordinates_standard() {
    val coords = listOf(Coordinates(10.0, 40.0), Coordinates(30.0, 20.0))
    val result = Bounds.fromCoordinates(coords)
    assertEquals(Bounds(south = 10.0, west = 20.0, north = 30.0, east = 40.0), result)
  }

  @Test
  fun fromCoordinates_antiMeridianCrossing() {
    val coords = listOf(Coordinates(0.0, 178.0), Coordinates(0.0, -179.0))
    val result = Bounds.fromCoordinates(coords)
    assertEquals(Bounds(south = 0.0, west = 178.0, north = 0.0, east = -179.0), result)
  }

  @Test
  fun fromGeometries_geometry() {
    val polygon =
      Polygon(
        LinearRing(
          listOf(
            Coordinates(10.0, 20.0),
            Coordinates(30.0, 20.0),
            Coordinates(30.0, 40.0),
            Coordinates(10.0, 20.0),
          )
        )
      )
    val result = Bounds.fromCoordinates(polygon.getShellCoordinates())
    assertEquals(Bounds(south = 10.0, west = 20.0, north = 30.0, east = 40.0), result)
    assertEquals(
      Bounds(south = 10.0, west = 20.0, north = 30.0, east = 40.0),
      Bounds.fromGeometries(listOf(polygon)),
    )
  }
}
