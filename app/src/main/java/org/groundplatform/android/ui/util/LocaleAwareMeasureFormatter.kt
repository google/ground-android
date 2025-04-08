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
import kotlin.math.roundToInt
import org.groundplatform.android.R
import timber.log.Timber

private const val METERS_TO_FEET = 3.28084
private const val DISTANCE_THRESHOLD = 10.0
private const val DECIMAL_MULTIPLIER = 100.0

class LocaleAwareMeasureFormatter @Inject constructor(val resources: Resources) {
  fun formatDistance(distance: Double): String {
    val locale = Locale.getDefault()
    val isImperial = isImperialSystem(locale)
    val (convertedDistance, unitLabel, measureUnit) = convertDistance(distance, isImperial)
    val roundedNumber = roundDistanceAsDouble(convertedDistance)
    val fallback = formatDistanceFallback(roundedNumber, unitLabel, locale)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      formatWithMeasureFormat(roundedNumber, measureUnit, fallback, locale)
    } else {
      fallback
    }
  }

  private fun convertDistance(
    distance: Double,
    isImperial: Boolean,
  ): Triple<Double, String, MeasureUnit> =
    if (isImperial) {
      Triple(distance * METERS_TO_FEET, resources.getString(R.string.unit_feet), MeasureUnit.FOOT)
    } else {
      Triple(distance, resources.getString(R.string.unit_meters), MeasureUnit.METER)
    }

  private fun roundDistanceAsDouble(distance: Double): Double =
    if (distance < DISTANCE_THRESHOLD) {
      (distance * DECIMAL_MULTIPLIER).roundToInt() / DECIMAL_MULTIPLIER
    } else {
      distance.roundToInt().toDouble()
    }

  private fun formatDistanceFallback(number: Double, unitLabel: String, locale: Locale): String {
    val formatted =
      if (number < 10) {
        String.format(locale, "%.2f", number)
      } else {
        String.format(locale, "%.0f", number)
      }
    return "$formatted $unitLabel"
  }

  private fun formatWithMeasureFormat(
    number: Double,
    measureUnit: MeasureUnit,
    fallback: String,
    locale: Locale,
  ): String =
    runCatching {
        val formatter =
          MeasureFormat.getInstance(ULocale.forLocale(locale), MeasureFormat.FormatWidth.SHORT)
        formatter.format(Measure(number, measureUnit))
      }
      .getOrElse { exception ->
        Timber.w("FormatDistance", "MeasureFormat failed", exception)
        fallback
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
