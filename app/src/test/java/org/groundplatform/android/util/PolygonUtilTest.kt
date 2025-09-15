/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.util

import junit.framework.TestCase.assertFalse
import kotlin.test.Test
import kotlin.test.assertTrue
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.ui.util.calculateShoelacePolygonArea
import org.groundplatform.android.ui.util.isClosed
import org.groundplatform.android.ui.util.isSelfIntersecting
import org.groundplatform.android.ui.util.segmentsIntersect
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PolygonUtilTest {

  @Test
  fun `segments intersect at a point`() {
    assertTrue(segmentsIntersect(P1 to P2, Q1 to Q2))
  }

  @Test
  fun `collinear but non-overlapping segments`() {
    assertFalse(segmentsIntersect(P1 to COLLINEAR1, Q2 to COLLINEAR2))
  }

  @Test
  fun `completely separate segments`() {
    assertFalse(segmentsIntersect(P1 to SEPARATE1, SEPARATE2 to Q2))
  }

  @Test
  fun `segments touching at an endpoint`() {
    assertTrue(segmentsIntersect(P1 to TOUCHING, TOUCHING to P2))
  }

  @Test
  fun `parallel but non-overlapping segments`() {
    assertFalse(
      segmentsIntersect(PARALLEL1 to PARALLEL2, Coordinates(5.0, 2.0) to Coordinates(7.0, 2.0))
    )
  }

  @Test
  fun `collinear and overlapping segments`() {
    assertTrue(segmentsIntersect(P1 to COLLINEAR2, COLLINEAR1 to P2))
  }

  @Test
  fun `non intersecting polygon`() {
    assertFalse(isSelfIntersecting(NON_INTERSECTING))
  }

  @Test
  fun `self intersecting polygon`() {
    assertTrue(isSelfIntersecting(listOf(P1, P2, Q1, Q2)))
  }

  @Test
  fun `triangle does not self intersect`() {
    assertFalse(isSelfIntersecting(TRIANGLE))
  }

  @Test
  fun `empty list`() {
    assertFalse(isSelfIntersecting(emptyList()))
  }

  @Test
  fun `single point`() {
    assertFalse(isSelfIntersecting(SINGLE_POINT))
  }

  @Test
  fun `three points`() {
    assertFalse(isSelfIntersecting(THREE_POINTS))
  }

  @Test
  fun `four points forming an x`() {
    assertTrue(isSelfIntersecting(FOUR_POINTS_X))
  }

  @Test
  fun `closed non-intersecting polygon does not self intersect`() {
    val closedSquare = NON_INTERSECTING + NON_INTERSECTING.first() // close the ring
    assertFalse(isSelfIntersecting(closedSquare))
  }

  @Test
  fun `adjacent edges sharing a vertex are not treated as intersection`() {
    val square =
      listOf(
        Coordinates(0.0, 0.0),
        Coordinates(1.0, 0.0),
        Coordinates(1.0, 1.0),
        Coordinates(0.0, 1.0),
        Coordinates(0.0, 0.0), // closed
      )
    assertFalse(isSelfIntersecting(square))
  }

  @Test
  fun `open polyline that crosses itself is detected`() {
    val openCross = listOf(P1, P2, Q1, Q2)
    assertTrue(isSelfIntersecting(openCross))
  }

  @Test
  fun `calculateShoelacePolygonArea should return correct area for simple square`() {
    val coordinates =
      listOf(
        Coordinates(0.0, 0.0),
        Coordinates(0.0, 0.00001),
        Coordinates(0.00001, 0.00001),
        Coordinates(0.00001, 0.0),
        Coordinates(0.0, 0.0), // Closing the polygon
      )

    val area = calculateShoelacePolygonArea(coordinates)
    assertEquals(1.24, area, 0.01) // Allowing minor floating-point error
  }

  @Test
  fun `calculateShoelacePolygonArea should return 0 for less than 3 points`() {
    val coordinates = listOf(Coordinates(24.523740, 73.606673), Coordinates(24.523736, 73.606803))

    val area = calculateShoelacePolygonArea(coordinates)
    assertEquals(0.0, area, 0.01)
  }

  @Test
  fun `isClosed returns true for closed polygon`() {
    val closedPolygon =
      listOf(
        Coordinates(0.0, 0.0),
        Coordinates(1.0, 0.0),
        Coordinates(1.0, 1.0),
        Coordinates(0.0, 1.0),
        Coordinates(0.0, 0.0), // Same as first
      )
    assertTrue(isClosed(closedPolygon))
  }

  @Test
  fun `isClosed returns false for open polygon`() {
    val openPolygon =
      listOf(
        Coordinates(0.0, 0.0),
        Coordinates(1.0, 0.0),
        Coordinates(1.0, 1.0),
        Coordinates(0.0, 1.0),
      )
    assertFalse(isClosed(openPolygon))
  }

  @Test
  fun `isClosed returns false for polygons with less than 4 points`() {
    assertFalse(isClosed(THREE_POINTS))
    assertFalse(isClosed(SINGLE_POINT))
    assertFalse(isClosed(emptyList()))
  }

  companion object {
    val P1 = Coordinates(1.0, 1.0)
    val P2 = Coordinates(4.0, 4.0)
    val Q1 = Coordinates(1.0, 4.0)
    val Q2 = Coordinates(4.0, 1.0)
    val COLLINEAR1 = Coordinates(3.0, 3.0)
    val COLLINEAR2 = Coordinates(6.0, 6.0)
    val TOUCHING = Coordinates(3.0, 3.0)
    val PARALLEL1 = Coordinates(1.0, 2.0)
    val PARALLEL2 = Coordinates(4.0, 2.0)
    val SEPARATE1 = Coordinates(2.0, 2.0)
    val SEPARATE2 = Coordinates(3.0, 3.0)
    val NON_INTERSECTING =
      listOf(
        Coordinates(0.0, 0.0),
        Coordinates(4.0, 0.0),
        Coordinates(4.0, 4.0),
        Coordinates(0.0, 4.0),
      )
    val TRIANGLE = listOf(Coordinates(0.0, 0.0), Coordinates(4.0, 0.0), Coordinates(2.0, 3.0))
    val SINGLE_POINT = listOf(Coordinates(1.0, 1.0))
    val THREE_POINTS = listOf(Coordinates(0.0, 0.0), Coordinates(4.0, 4.0), Coordinates(8.0, 0.0))
    val FOUR_POINTS_X = listOf(P1, P2, Q1, Q2, P1) // Closing the polygon
  }
}
