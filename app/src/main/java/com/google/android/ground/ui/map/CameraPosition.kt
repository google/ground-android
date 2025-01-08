/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.ui.map

import com.google.android.ground.model.geometry.Coordinates
import timber.log.Timber

/** Represents current camera position of the map. */
data class CameraPosition(
  val coordinates: Coordinates,
  val zoomLevel: Float? = null,
  val bounds: Bounds? = null,
) {

  fun serialize(): String =
    arrayOf<Any>(
        coordinates.lat.toString(),
        coordinates.lng.toString(),
        zoomLevel.toString(),
        bounds?.south.toString(),
        bounds?.west.toString(),
        bounds?.north.toString(),
        bounds?.east.toString(),
      )
      .joinToString { it.toString() }

  companion object {

    fun deserialize(serializedValue: String): CameraPosition? =
      runCatching {
          if (serializedValue.isEmpty()) return null
          val parts = serializedValue.split(",")
          val lat = parts[0].trim().toDouble()
          val long = parts[1].trim().toDouble()
          val zoomLevel = parts[2].trim().toFloatOrNull()
          val south = parts[3].trim().toDoubleOrNull()
          val west = parts[4].trim().toDoubleOrNull()
          val north = parts[5].trim().toDoubleOrNull()
          val east = parts[6].trim().toDoubleOrNull()

          var bounds: Bounds? = null
          if (south != null && west != null && north != null && east != null) {
            bounds = Bounds(south, west, north, east)
          }

          return CameraPosition(Coordinates(lat, long), zoomLevel, bounds)
        }
        .getOrElse { exception ->
          Timber.e(exception)
          // Prevent app from crashing if we are unable to parse the camera position
          null
        }
  }
}
