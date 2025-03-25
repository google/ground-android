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

fun formatDistance(resources: Resources, distanceInMeters: Double): String {
  val isImperial =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val measurementSystem = LocaleData.getMeasurementSystem(ULocale.getDefault())
      measurementSystem == LocaleData.MeasurementSystem.US
    } else {
      isImperialSystemFallback()
    }

  val (convertedDistance, unitString, unitMeasure) =
    if (isImperial) {
      Triple(distanceInMeters * 3.28084, resources.getString(R.string.unit_feet), MeasureUnit.FOOT)
    } else {
      Triple(distanceInMeters, resources.getString(R.string.unit_meters), MeasureUnit.METER)
    }

  val roundedDistance =
    if (convertedDistance < 10) {
      "%.2f".format(Locale.US, convertedDistance)
    } else {
      "%.0f".format(Locale.US, convertedDistance)
    }

  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    val formatter = MeasureFormat.getInstance(ULocale.getDefault(), MeasureFormat.FormatWidth.SHORT)
    formatter.format(Measure(roundedDistance.toDouble(), unitMeasure))
  } else {
    "$roundedDistance $unitString"
  }
}

private fun isImperialSystemFallback(): Boolean {
  val country = Locale.getDefault().country.uppercase(Locale.ROOT)
  return country in listOf("US", "LR", "MM") // Countries using the imperial system
}
