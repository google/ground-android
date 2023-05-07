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

package com.google.android.ground.ui.map.gms.mog

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.lang.Math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

/**
 * Uniquely identifies the coordinates of a web mercator tile by its X and Y coordinates and its
 * respective zoom level.
 */
data class TileCoordinates(val x: Int, val y: Int, val zoom: Int) {
  /** Returns the coordinates of the tile at [targetZoom] with the same northwest corner. */
  fun originAtZoom(targetZoom: Int): TileCoordinates {
    val zoomDelta = targetZoom - zoom
    val scale = { x: Int -> if (zoomDelta > 0) x shl zoomDelta else x shr abs(zoomDelta) }
    return TileCoordinates(scale(x), scale(y), targetZoom)
  }

  override fun toString(): String {
    return "($x, $y) at zoom $zoom"
  }

  companion object {
    /** Tile coordinates of the world as a single tile (0, 0) at zoom level 0. */
    val WORLD = TileCoordinates(0, 0, 0)

    /**
     * Returns the coordinates of the tile at a particular zoom containing the specified latitude
     * and longitude coordinates.
     */
    fun fromLatLng(coordinates: LatLng, zoom: Int): TileCoordinates {
      val zoomFactor = 1 shl zoom
      val latRad = coordinates.latitude.toRadians()
      val x = zoomFactor * (coordinates.longitude + 180) / 360
      val y = zoomFactor * (1 - (ln(tan(latRad) + sec(latRad)) / PI)) / 2
      return TileCoordinates(x.toInt(), y.toInt(), zoom)
    }

    /**
     * Returns all tiles at a particular zoom contained within the specified latitude and longitude
     * bounds.
     */
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

/** Returns the secant of angle `x` given in radians. */
private fun sec(x: Double) = 1 / cos(x)

/** Converts degrees into radians. */
private fun Double.toRadians() = this * (PI / 180)
