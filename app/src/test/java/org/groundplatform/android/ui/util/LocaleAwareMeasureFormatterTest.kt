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
package org.groundplatform.android.ui.util

import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import junit.framework.TestCase.assertTrue
import kotlin.test.Test
import org.groundplatform.android.BaseHiltTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocaleAwareMeasureFormatterTest : BaseHiltTest() {

  @Test
  fun `formatDistance returns metric formatting for small distances`() {
    val formatter = LocaleAwareMeasureFormatter(Locale("en", "GB"))
    val distanceInMeters = 3.5

    val result = formatter.formatDistance(distanceInMeters)

    assertTrue("Formatted metric distance should contain \"3.5\"", result.contains("3.5"))
    assertTrue(
      "Formatted metric distance should contain a meter unit",
      result.lowercase().contains("m"),
    )
  }

  @Test
  fun `formatDistance returns metric formatting for large distances`() {
    val formatter = LocaleAwareMeasureFormatter(Locale("en", "GB"))
    val distanceInMeters = 10.9
    val result = formatter.formatDistance(distanceInMeters)

    assertTrue("Formatted metric large distance should contain \"10\"", result.contains("10"))
    assertTrue(
      "Formatted metric large distance should contain a meter unit",
      result.lowercase().contains("m"),
    )
  }

  @Test
  fun `formatDistance returns imperial formatting for small distances`() {
    val formatter = LocaleAwareMeasureFormatter(Locale("en", "US"))
    val distanceInMeters = 3.0
    val result = formatter.formatDistance(distanceInMeters)

    assertTrue("Formatted imperial small distance should contain \"9.8\"", result.contains("9.8"))
    assertTrue(
      "Formatted imperial small distance should contain a foot unit",
      result.lowercase().contains("ft"),
    )
  }

  @Test
  fun `formatDistance returns imperial formatting for large distances`() {
    val formatter = LocaleAwareMeasureFormatter(Locale("en", "US"))
    val distanceInMeters = 10.0
    val result = formatter.formatDistance(distanceInMeters)

    assertTrue("Formatted imperial large distance should contain \"32\"", result.contains("32"))
    assertTrue(
      "Formatted imperial large distance should contain a foot unit",
      result.lowercase().contains("ft"),
    )
  }
}
