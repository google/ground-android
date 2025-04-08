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

import android.content.res.Resources
import android.icu.text.MeasureFormat
import android.icu.util.LocaleData
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.icu.util.ULocale
import android.os.Build
import java.util.Locale
import javax.inject.Inject
import timber.log.Timber

private const val METERS_TO_FEET = 3.28084

class LocaleAwareMeasureFormatter @Inject constructor(val resources: Resources) {
  fun formatDistance(distanceInMeters: Double): String {
    val locale = Locale.getDefault()
    val isImperial = isImperialSystem(locale)
    val distance = if (isImperial) distanceInMeters.toFeet() else distanceInMeters
    val measureUnit = if (isImperial) MeasureUnit.FOOT else MeasureUnit.METER
    return formatWithMeasureFormat(distance, measureUnit, locale)
  }

  private fun Double.toFeet() = this * METERS_TO_FEET

  private fun formatWithMeasureFormat(
    number: Double,
    measureUnit: MeasureUnit,
    locale: Locale,
  ): String {
    val formatter =
      MeasureFormat.getInstance(ULocale.forLocale(locale), MeasureFormat.FormatWidth.SHORT)
    return formatter.format(Measure(number, measureUnit))
  }

  private fun isImperialSystem(locale: Locale): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      runCatching {
          LocaleData.getMeasurementSystem(ULocale.forLocale(locale)) ==
            LocaleData.MeasurementSystem.US
        }
        .getOrElse { exception ->
          Timber.w(
            "MeasurementSystem",
            "Failed to get measurement system from LocaleData",
            exception,
          )
          isImperialSystemFallback(locale)
        }
    } else {
      isImperialSystemFallback(locale)
    }

  private fun isImperialSystemFallback(locale: Locale): Boolean =
    locale.country.uppercase(Locale.ROOT) in setOf("US", "LR", "MM")
}
