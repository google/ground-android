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
  fun shellCoordinates_returnsExpectedCoordinates() {
    val c1 = Coordinates(1.0, 2.0)
    val c2 = Coordinates(3.0, 4.0)
    val c3 = Coordinates(1.0, 2.0)

    val point = Point(c1)
    assertEquals(listOf(c1), point.shellCoordinates)

    val lineString = LineString.lineStringOf(c1, c2)
    assertEquals(listOf(c1, c2), lineString.shellCoordinates)

    val linearRing = LinearRing(listOf(c1, c2, c3))
    assertEquals(listOf(c1, c2, c3), linearRing.shellCoordinates)

    val polygon = Polygon(linearRing)
    assertEquals(listOf(c1, c2, c3), polygon.shellCoordinates)

    val multiPolygon = MultiPolygon(listOf(polygon))
    assertEquals(listOf(c1, c2, c3), multiPolygon.shellCoordinates)
  }

  @Test
  fun center_standardBounds() {
    assertEquals(Coordinates(20.0, 30.0), bounds.center)
    @Suppress("DEPRECATION") assertEquals(Coordinates(20.0, 30.0), bounds.center())
  }

  @Test
  fun center_antiMeridianCrossingBounds() {
    val antiMeridianBounds = Bounds(south = -10.0, west = 170.0, north = 10.0, east = -170.0)
    assertEquals(0.0, antiMeridianBounds.center.lat)
    val lng = antiMeridianBounds.center.lng
    assertTrue(lng == 180.0 || lng == -180.0)
  }

  @Test
  fun center_antiMeridianCrossing_lngGreaterThan180SubBranch() {
    // west = 170.0, east = -150.0.
    // (170.0 + -150.0 + 360.0) / 2.0 = 190.0 > 180.0 -> lng becomes 190.0 - 360.0 = -170.0
    val bounds = Bounds(south = -10.0, west = 170.0, north = 10.0, east = -150.0)
    assertEquals(Coordinates(0.0, -170.0), bounds.center)
    @Suppress("DEPRECATION") assertEquals(Coordinates(0.0, -170.0), bounds.center())
  }

  @Test
  fun center_negativeLongitudesAndEquatorCrossing() {
    val bounds = Bounds(south = -30.0, west = -120.0, north = 10.0, east = -60.0)
    assertEquals(Coordinates(-10.0, -90.0), bounds.center)
  }

  @Test
  fun center_zeroWidthBounds() {
    val singlePointBounds = Bounds(south = 15.0, west = 25.0, north = 15.0, east = 25.0)
    assertEquals(Coordinates(15.0, 25.0), singlePointBounds.center)
  }

  @Test
  fun shrink_standardBounds() {
    val shrunk = bounds.shrink(0.5)
    // original: south = 10, west = 20, north = 30, east = 40. latSpan = 20, lngSpan = 20.
    // offset = 20 * 0.5 * 0.5 = 5.
    assertEquals(Bounds(south = 15.0, west = 25.0, north = 25.0, east = 35.0), shrunk)
  }

  @Test
  fun shrink_antiMeridianCrossingBounds() {
    val bounds = Bounds(south = -10.0, west = 170.0, north = 10.0, east = -170.0)
    // span = 20. offset = 5. newWest = 175, newEast = -175.
    val shrunk = bounds.shrink(0.5)
    assertEquals(Bounds(south = -5.0, west = 175.0, north = 5.0, east = -175.0), shrunk)
  }

  @Test
  fun contains_coordinates_antiMeridianBoundaries() {
    val antiMeridianBounds = Bounds(south = -10.0, west = 170.0, north = 10.0, east = -170.0)

    assertTrue(antiMeridianBounds.contains(Coordinates(0.0, 170.0)))
    assertTrue(antiMeridianBounds.contains(Coordinates(0.0, -170.0)))
    assertTrue(antiMeridianBounds.contains(Coordinates(0.0, 180.0)))
    assertTrue(antiMeridianBounds.contains(Coordinates(0.0, -180.0)))
    assertFalse(antiMeridianBounds.contains(Coordinates(0.0, 169.9999)))
    assertFalse(antiMeridianBounds.contains(Coordinates(0.0, -169.9999)))

    // Latitude out of range when lng is inside
    assertFalse(antiMeridianBounds.contains(Coordinates(15.0, 175.0)))
    assertFalse(antiMeridianBounds.contains(Coordinates(-15.0, -175.0)))
  }

  @Test
  fun contains_coordinates_zeroWidthBounds() {
    val zeroBounds = Bounds(south = 10.0, west = 20.0, north = 10.0, east = 20.0)
    assertTrue(zeroBounds.contains(Coordinates(10.0, 20.0)))
    assertFalse(zeroBounds.contains(Coordinates(10.0, 20.001)))
    assertFalse(zeroBounds.contains(Coordinates(10.001, 20.0)))
  }

  @Test
  fun contains_linearRingGeometry() {
    val ringInside =
      LinearRing(listOf(Coordinates(15.0, 25.0), Coordinates(25.0, 25.0), Coordinates(15.0, 25.0)))
    val ringOutside =
      LinearRing(listOf(Coordinates(0.0, 0.0), Coordinates(5.0, 0.0), Coordinates(0.0, 0.0)))
    val emptyRing = LinearRing(emptyList())

    assertTrue(bounds.contains(ringInside))
    assertFalse(bounds.contains(ringOutside))
    assertFalse(bounds.contains(emptyRing))
  }

  @Test
  fun contains_multiPolygonGeometry() {
    val shellOutside =
      LinearRing(listOf(Coordinates(0.0, 0.0), Coordinates(5.0, 0.0), Coordinates(0.0, 0.0)))
    val shellInside =
      LinearRing(listOf(Coordinates(20.0, 30.0), Coordinates(25.0, 30.0), Coordinates(20.0, 30.0)))

    val multiPolygonAllOutside = MultiPolygon(listOf(Polygon(shellOutside)))
    val multiPolygonPartialInside =
      MultiPolygon(listOf(Polygon(shellOutside), Polygon(shellInside)))
    val emptyMultiPolygon = MultiPolygon(emptyList())

    assertFalse(bounds.contains(multiPolygonAllOutside))
    assertTrue(bounds.contains(multiPolygonPartialInside))
    assertFalse(bounds.contains(emptyMultiPolygon))
  }

  @Test
  fun contains_geometry_emptyLineStringReturnsFalse() {
    assertFalse(bounds.contains(LineString(emptyList())))
  }

  @Test
  fun contains_polygon_holeVerticesInsideIgnored() {
    val shellOutside =
      LinearRing(
        listOf(
          Coordinates(0.0, 0.0),
          Coordinates(0.0, 5.0),
          Coordinates(5.0, 5.0),
          Coordinates(5.0, 0.0),
          Coordinates(0.0, 0.0),
        )
      )
    val holeInside =
      LinearRing(
        listOf(
          Coordinates(20.0, 30.0),
          Coordinates(20.0, 35.0),
          Coordinates(25.0, 35.0),
          Coordinates(20.0, 30.0),
        )
      )
    val polygon = Polygon(shellOutside, listOf(holeInside))
    assertFalse(bounds.contains(polygon))
  }

  @Test
  fun fromCoordinates_emptyCoordinates_returnsNull() {
    assertEquals(null, Bounds.fromCoordinates(emptyList()))
    assertEquals(null, Bounds.fromGeometries(emptyList()))
  }

  @Test
  fun fromCoordinates_singleCoordinate() {
    val single = listOf(Coordinates(12.0, 34.0))
    val result = Bounds.fromCoordinates(single)
    assertEquals(Bounds(south = 12.0, west = 34.0, north = 12.0, east = 34.0), result)
  }

  @Test
  fun fromCoordinates_standard() {
    val coords = listOf(Coordinates(10.0, 40.0), Coordinates(30.0, 20.0))
    val result = Bounds.fromCoordinates(coords)
    assertEquals(Bounds(south = 10.0, west = 20.0, north = 30.0, east = 40.0), result)
  }

  @Test
  fun fromCoordinates_containedPointsDoNotExpandBounds() {
    val coords =
      listOf(
        Coordinates(10.0, 20.0),
        Coordinates(30.0, 40.0),
        Coordinates(20.0, 30.0),
      )
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
  fun fromCoordinates_antiMeridianContainedPointsAndExpansion() {
    val coords =
      listOf(
        Coordinates(0.0, 175.0),
        Coordinates(0.0, -175.0),
        Coordinates(0.0, 178.0),
        Coordinates(0.0, -178.0),
        Coordinates(-10.0, 170.0),
        Coordinates(10.0, -170.0),
      )
    val result = Bounds.fromCoordinates(coords)
    assertEquals(Bounds(south = -10.0, west = 170.0, north = 10.0, east = -170.0), result)
  }

  @Test
  fun fromCoordinates_antiMeridianDualRepresentationNoExplosion() {
    val coords = listOf(Coordinates(0.0, -180.0), Coordinates(0.0, 180.0))
    val result = Bounds.fromCoordinates(coords)
    assertEquals(Bounds(south = 0.0, west = -180.0, north = 0.0, east = -180.0), result)
  }

  @Test
  fun fromCoordinates_exactOppositeLongitudesTieBreaker() {
    val coords = listOf(Coordinates(0.0, 0.0), Coordinates(0.0, 180.0))
    val result = Bounds.fromCoordinates(coords)
    assertEquals(Bounds(south = 0.0, west = 0.0, north = 0.0, east = 180.0), result)
  }

  @Test
  fun fromGeometries_geometriesWithNoCoordinatesReturnsNull() {
    val emptyGeometries =
      listOf(LineString(emptyList()), LinearRing(emptyList()), MultiPolygon(emptyList()))
    assertEquals(null, Bounds.fromGeometries(emptyGeometries))
  }

  @Test
  fun fromGeometries_heterogeneousCollection() {
    val point = Point(Coordinates(-10.0, -20.0))
    val lineString = LineString.lineStringOf(Coordinates(10.0, 20.0), Coordinates(30.0, 40.0))
    val result = Bounds.fromGeometries(listOf(point, lineString))
    assertEquals(Bounds(south = -10.0, west = -20.0, north = 30.0, east = 40.0), result)
  }

  @Test
  fun fromGeometry_singleGeometry() {
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
    val result = Bounds.fromGeometry(polygon)
    assertEquals(Bounds(south = 10.0, west = 20.0, north = 30.0, east = 40.0), result)
  }
}
