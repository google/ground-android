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

import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.ui.util.calculateShoelacePolygonArea
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PolygonUtilTest {

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
}
