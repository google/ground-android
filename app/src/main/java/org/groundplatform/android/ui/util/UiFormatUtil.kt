/*
 * Copyright 2026 Google LLC
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

import androidx.annotation.VisibleForTesting
import java.util.Locale
import org.groundplatform.domain.model.settings.MeasurementUnits

@VisibleForTesting const val SQUARE_METERS_PER_ACRE = 4046.86
@VisibleForTesting const val SQUARE_METERS_PER_HECTARE = 10_000
@VisibleForTesting const val SQUARE_FEET_PER_SQUARE_METER = 10.7639

fun getFormattedArea(areaInSquareMeters: Double, measurementUnits: MeasurementUnits): String {
  val (convertedArea, stringUnit) =
    when (measurementUnits) {
      MeasurementUnits.METRIC ->
        if (areaInSquareMeters < SQUARE_METERS_PER_HECTARE) {
          areaInSquareMeters to "m²"
        } else {
          areaInSquareMeters / SQUARE_METERS_PER_HECTARE to "ha"
        }
      MeasurementUnits.IMPERIAL ->
        if (areaInSquareMeters < SQUARE_METERS_PER_ACRE) {
          areaInSquareMeters * SQUARE_FEET_PER_SQUARE_METER to "ft²"
        } else {
          areaInSquareMeters / SQUARE_METERS_PER_ACRE to "ac"
        }
    }
  val rounded = String.format(Locale.getDefault(), "%.2f", convertedArea)
  return "$rounded $stringUnit"
}
