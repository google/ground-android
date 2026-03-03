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

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import kotlin.test.Test
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.ui.util.FormatterUtils.SQUARE_FEET_PER_SQUARE_METER
import org.groundplatform.android.ui.util.FormatterUtils.SQUARE_METERS_PER_ACRE
import org.groundplatform.android.ui.util.FormatterUtils.SQUARE_METERS_PER_HECTARE
import org.groundplatform.android.ui.util.FormatterUtils.getFormattedArea
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormatterUtilsTest {
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
    val expected =
      String.format(
        Locale.getDefault(),
        "%.2f ft²",
        areaInSquareMeters * SQUARE_FEET_PER_SQUARE_METER,
      )
    assertEquals(expected, result)
  }

  @Test
  fun `Should display area in acres if it's equal or above an acre`() {
    val areaInSquareMeters = SQUARE_METERS_PER_ACRE
    val result = getFormattedArea(areaInSquareMeters, MeasurementUnits.IMPERIAL)
    assertEquals("1.00 ac", result)
  }
}
