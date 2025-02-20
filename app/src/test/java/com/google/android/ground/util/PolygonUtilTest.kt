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
package com.google.android.ground.util

import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.ui.util.segmentsIntersect
import junit.framework.TestCase.assertFalse
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PolygonUtilTest {

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
  }

  @Test
  fun `segments intersect at a point`() {
    assertTrue(segmentsIntersect(P1, P2, Q1, Q2))
  }

  @Test
  fun `collinear but non-overlapping segments`() {
    assertFalse(segmentsIntersect(P1, COLLINEAR1, Q2, COLLINEAR2))
  }

  @Test
  fun `completely separate segments`() {
    assertFalse(segmentsIntersect(P1, SEPARATE1, SEPARATE2, Q2))
  }

  @Test
  fun `segments touching at an endpoint`() {
    assertTrue(segmentsIntersect(P1, TOUCHING, TOUCHING, P2))
  }

  @Test
  fun `parallel but non-overlapping segments`() {
    assertFalse(
      segmentsIntersect(PARALLEL1, PARALLEL2, Coordinates(5.0, 2.0), Coordinates(7.0, 2.0))
    )
  }

  @Test
  fun `collinear and overlapping segments`() {
    assertTrue(segmentsIntersect(P1, COLLINEAR2, COLLINEAR1, P2))
  }
}
