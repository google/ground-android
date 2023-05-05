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

package com.google.android.ground.ui.map.gms.tcog

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.lang.Math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

private fun sec(x: Double) = 1 / cos(x)

fun Double.toRadians() = this * (PI / 180)

data class TileCoordinates(val x: Int, val y: Int, val zoom: Int) {
  fun originAtZoom(targetZoom: Int): TileCoordinates {
    val zoomDelta = targetZoom - zoom
    return if (zoomDelta > 0) {
      TileCoordinates(x shl zoomDelta, y shl zoomDelta, targetZoom)
    } else {
      TileCoordinates(x shr abs(zoomDelta), y shr abs(zoomDelta), targetZoom)
    }
  }

  override fun toString(): String {
    return "($x, $y) @ $zoom"
  }

  companion object {
    fun fromLatLng(coords: LatLng, zoom: Int): TileCoordinates {
      val zoomFactor = 1 shl zoom
      val latRad = coords.latitude.toRadians()
      val x = zoomFactor * (coords.longitude + 180) / 360
      val y = zoomFactor * (1 - (ln(tan(latRad) + sec(latRad)) / PI)) / 2
      return TileCoordinates(x.toInt(), y.toInt(), zoom)
    }

    fun withinBounds(bounds: LatLngBounds, zoom: Int): List<TileCoordinates> {
      val results = mutableListOf<TileCoordinates>()
      val nwTile = fromLatLng(bounds.northwest(), zoom)
      val seTile = fromLatLng(bounds.southeast(), zoom)
      for (y in nwTile.y..seTile.y) {
        for (x in nwTile.x..seTile.x) {
          results.add(TileCoordinates(x, y, zoom))
        }
      }
      return results.toList()
    }
  }
}
