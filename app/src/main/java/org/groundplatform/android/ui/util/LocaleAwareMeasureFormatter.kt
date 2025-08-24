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

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.icu.util.ULocale
import java.util.Locale
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.pow

private const val METERS_TO_FEET = 3.28084

/** Distances equal to or greater than this value are rounded down to the nearest integer. */
private const val MIN_ROUNDED_DISTANCE = 10

/** The number of decimal places shown for distances < MIN_ROUNDED_DISTANCE. */
private const val SMALL_DISTANCE_DECIMAL_PLACES = 1

class LocaleAwareMeasureFormatter @Inject constructor() {
  private val uLocale = ULocale.forLocale(Locale.getDefault())
  private val distanceFormatter =
    MeasureFormat.getInstance(uLocale, MeasureFormat.FormatWidth.SHORT)

  fun formatDistance(distanceInMeters: Double, unit: MeasureUnit): String {
    val distance = if (unit == MeasureUnit.FOOT) distanceInMeters.toFeet() else distanceInMeters
    val roundedDistance = getRoundedDistance(distance)
    return distanceFormatter.format(Measure(roundedDistance, unit))
  }

  private fun getRoundedDistance(distance: Double): Double =
    if (distance < MIN_ROUNDED_DISTANCE) {
      distance.floorToDecimalPlaces(SMALL_DISTANCE_DECIMAL_PLACES)
    } else {
      floor(distance)
    }

  private fun Double.toFeet() = this * METERS_TO_FEET

  private fun Double.floorToDecimalPlaces(n: Int): Double {
    val factor = 10.0.pow(n)
    return floor(this * factor) / factor
  }
}
