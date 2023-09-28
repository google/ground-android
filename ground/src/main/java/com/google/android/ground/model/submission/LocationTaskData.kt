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
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.ui.datacollection.tasks.point.LatLngConverter
import java.math.RoundingMode
import java.text.DecimalFormat

data class LocationTaskData(
  val geometry: Geometry?,
  val altitude: Double?, // in metres
  val accuracy: Double? // in metres
) : TaskData {
  override fun getDetailsText(): String {
    if (geometry !is Point) {
      return "Invalid geometry type: Expected POINT, Found ${geometry?.javaClass?.name}"
    }

    // TODO: Move to strings.xml for i18n
    val df = DecimalFormat("#.##")
    df.roundingMode = RoundingMode.DOWN
    return "${LatLngConverter.processCoordinates(geometry.coordinates)}\n" +
      "Altitude: ${df.format(altitude)}m\n" +
      "Accuracy: ${df.format(accuracy)}m"
  }

  override fun isEmpty(): Boolean = geometry == null

  companion object {
    fun fromLocation(location: Location?): LocationTaskData? {
      if (location == null) return null
      with(location) {
        val altitude = if (hasAltitude()) altitude else null
        val accuracy = if (hasAccuracy()) accuracy else null
        return LocationTaskData(
          Point(Coordinates(latitude, longitude)),
          altitude,
          accuracy?.toDouble()
        )
      }
    }
  }
}
