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

import android.location.Location
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.util.toDmsFormat
import java.math.RoundingMode
import java.text.DecimalFormat

/** User-provided response to a "capture location" data collection [Task]. */
data class CaptureLocationTaskData(
  val location: Point,
  val altitude: Double?, // in metres
  val accuracy: Double?, // in metres
) : GeometryTaskData(location) {
  override fun getDetailsText(): String {
    // TODO(https://github.com/google/ground-android/issues/1733): Move to strings.xml for i18n
    val df = DecimalFormat("#.##")
    df.roundingMode = RoundingMode.DOWN
    val coordinatesString = location.coordinates.toDmsFormat()
    val altitudeString = altitude?.let { df.format(it) } ?: "?"
    val accuracyString = accuracy?.let { df.format(it) } ?: "?"
    return "$coordinatesString\nAltitude: $altitudeString m\nAccuracy: $accuracyString m"
  }

  override fun isEmpty(): Boolean = false

  companion object {
    fun Location.toCaptureLocationResult(): CaptureLocationTaskData {
      val altitude = if (hasAltitude()) altitude else null
      val accuracy = if (hasAccuracy()) accuracy else null
      return CaptureLocationTaskData(
        Point(Coordinates(latitude, longitude)),
        altitude,
        accuracy?.toDouble(),
      )
    }
  }
}
