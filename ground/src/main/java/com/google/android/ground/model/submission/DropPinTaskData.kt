/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.model.submission

import com.google.android.ground.model.geometry.LatLngConverter
import com.google.android.ground.model.geometry.Point
import java.math.RoundingMode
import java.text.DecimalFormat

/** User-provided response to a "drop a pin" data collection [Task]. */
data class DropPinTaskData(val location: Point) : GeometryTaskData(location) {
  override fun getDetailsText(): String {
    // TODO(#752): Move to strings.xml for i18n
    val df = DecimalFormat("#.##")
    df.roundingMode = RoundingMode.DOWN
    return LatLngConverter.formatCoordinates(location.coordinates) ?: ""
  }

  override fun isEmpty(): Boolean = false
}
