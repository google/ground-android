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
import org.groundplatform.android.R
import timber.log.Timber

private const val METERS_TO_FEET = 3.28084

fun formatDistance(resources: Resources, distanceInMeters: Double): String {
  if (distanceInMeters < 0) return "" // ✅ Prevent negative distances

  val isImperial = isImperialSystem()

  val (convertedDistance, unitLabel, measureUnit) =
    if (isImperial) {
      Triple(distanceInMeters.toFeet(), resources.getString(R.string.unit_feet), MeasureUnit.FOOT)
    } else {
      Triple(distanceInMeters, resources.getString(R.string.unit_meters), MeasureUnit.METER)
    }

  val rounded =
    if (convertedDistance < 10) {
      String.format(Locale.getDefault(), "%.2f", convertedDistance)
    } else {
      String.format(Locale.getDefault(), "%.0f", convertedDistance)
    }

  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    formatWithMeasureFormat(rounded, measureUnit, "$rounded $unitLabel")
  } else {
    "$rounded $unitLabel"
  }
}

private fun formatWithMeasureFormat(value: String, unit: MeasureUnit, fallback: String): String {
  return try {
    val formatter = MeasureFormat.getInstance(ULocale.getDefault(), MeasureFormat.FormatWidth.SHORT)
    val number = value.toDoubleOrNull() ?: return fallback
    formatter.format(Measure(number, unit))
  } catch (e: Exception) {
    Timber.w("FormatDistance", "MeasureFormat failed", e)
    fallback
  }
}

private fun Double.toFeet(): Double = this * METERS_TO_FEET

private fun isImperialSystem(): Boolean =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    try {
      LocaleData.getMeasurementSystem(ULocale.getDefault()) == LocaleData.MeasurementSystem.US
    } catch (e: Exception) {
      Timber.w("MeasurementSystem", "Failed to get measurement system from LocaleData", e)
      isImperialSystemFallback()
    }
  } else {
    isImperialSystemFallback()
  }

private fun isImperialSystemFallback(): Boolean {
  val country = Locale.getDefault().country.uppercase(Locale.ROOT)
  return country in setOf("US", "LR", "MM")
}
