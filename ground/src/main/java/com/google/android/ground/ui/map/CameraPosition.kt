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

import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.geometry.Point

data class CameraPosition(
  val target: Point,
  val zoomLevel: Float? = null,
  val isAllowZoomOut: Boolean = false, // TODO: Handle serialization/deserialization
  var bounds: LatLngBounds? = null // TODO: Handle serialization/deserialization
) {

  override fun toString(): String {
    return "Position: $target Zoom level: $zoomLevel"
  }

  fun serialize(): String =
    arrayOf<Any>(target.coordinate.x, target.coordinate.y, zoomLevel ?: "").joinToString {
      it.toString()
    }

  companion object {

    fun deserialize(serializedValue: String): CameraPosition? {
      if (serializedValue.isEmpty()) return null
      val (lat, long, zoomLevel) = serializedValue.split(",")
      return CameraPosition(Point(Coordinate(lat.toDouble(), long.toDouble())), zoomLevel.toFloat())
    }
  }
}
