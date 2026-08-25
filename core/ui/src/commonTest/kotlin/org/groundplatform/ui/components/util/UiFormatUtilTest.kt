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
package org.groundplatform.ui.components.util

import kotlin.test.Test
import kotlin.test.assertEquals
import org.groundplatform.domain.model.settings.MeasurementUnits
import org.groundplatform.domain.util.Constants.SQUARE_METERS_PER_ACRE
import org.groundplatform.domain.util.Constants.SQUARE_METERS_PER_HECTARE
import org.groundplatform.ui.util.getFormattedArea

class UiFormatUtilTest {
  @Test
  fun `Should display area in square meters if it's below an hectare`() {
    val areaInSquareMeters = 500.0
    val result = getFormattedArea(areaInSquareMeters, MeasurementUnits.METRIC)
    assertEquals("500.00 m²", result)
  }

  @Test
  fun `Should display area in hectare if it's equal or above an hectare`() {
    val areaInSquareMeters = SQUARE_METERS_PER_HECTARE.toDouble()
    val result = getFormattedArea(areaInSquareMeters, MeasurementUnits.METRIC)
    assertEquals("1.00 ha", result)
  }

  @Test
  fun `Should display area in square feet if it's below an acre`() {
    val areaInSquareMeters = 2000.0
    val result = getFormattedArea(areaInSquareMeters, MeasurementUnits.IMPERIAL)
    val expected = "21527.80 ft²"
    assertEquals(expected, result)
  }

  @Test
  fun `Should display area in acres if it's equal or above an acre`() {
    val areaInSquareMeters = SQUARE_METERS_PER_ACRE
    val result = getFormattedArea(areaInSquareMeters, MeasurementUnits.IMPERIAL)
    assertEquals("1.00 ac", result)
  }
}
