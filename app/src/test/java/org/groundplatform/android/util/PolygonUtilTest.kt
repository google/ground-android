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
import org.groundplatform.android.ui.util.isIntersecting
import org.groundplatform.android.ui.util.isSelfIntersecting
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PolygonUtilTest {

  @Test
  fun `segments intersect at a point`() {
    assertTrue(isIntersecting(P1, P2, Q1, Q2))
  }

  @Test
  fun `collinear but non-overlapping segments`() {
    assertFalse(isIntersecting(P1, COLLINEAR1, Q2, COLLINEAR2))
  }

  @Test
  fun `completely separate segments`() {
    assertFalse(isIntersecting(P1, SEPARATE1, SEPARATE2, Q2))
  }

  @Test
  fun `segments touching at an endpoint`() {
    assertTrue(isIntersecting(P1, TOUCHING, TOUCHING, P2))
  }

  @Test
  fun `parallel but non-overlapping segments`() {
    assertFalse(isIntersecting(PARALLEL1, PARALLEL2, Coordinates(5.0, 2.0), Coordinates(7.0, 2.0)))
  }

  @Test
  fun `collinear and overlapping segments`() {
    assertTrue(isIntersecting(P1, COLLINEAR2, COLLINEAR1, P2))
  }

  @Test
  fun testNonIntersectingPolygon() {
    assertFalse(isSelfIntersecting(NON_INTERSECTING))
  }

  @Test
  fun testSelfIntersectingPolygon() {
    assertTrue(isSelfIntersecting(listOf(P1, P2, Q1, Q2)))
  }

  @Test
  fun testTriangle() {
    assertFalse(isSelfIntersecting(TRIANGLE))
  }

  @Test
  fun testEmptyList() {
    assertFalse(isSelfIntersecting(emptyList()))
  }

  @Test
  fun testSinglePoint() {
    assertFalse(isSelfIntersecting(SINGLE_POINT))
  }

  @Test
  fun testThreePoints() {
    assertFalse(isSelfIntersecting(THREE_POINTS))
  }

  @Test
  fun testFourPointsFormingAnX() {
    assertTrue(isSelfIntersecting(FOUR_POINTS_X))
  }

  @org.junit.Test
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

  @org.junit.Test
  fun `calculateShoelacePolygonArea should return 0 for less than 3 points`() {
    val coordinates = listOf(Coordinates(24.523740, 73.606673), Coordinates(24.523736, 73.606803))

    val area = calculateShoelacePolygonArea(coordinates)
    assertEquals(0.0, area, 0.01)
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
