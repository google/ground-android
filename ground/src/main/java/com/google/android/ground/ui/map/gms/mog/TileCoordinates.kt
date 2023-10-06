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

import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.ui.map.Bounds
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
    return TileCoordinates(x.shiftLeft(zoomDelta), y.shiftLeft(zoomDelta), targetZoom)
  }

  override fun toString(): String = "($x, $y) at zoom $zoom"

  companion object {
    /**
     * Returns the coordinates of the tile at a particular zoom containing the specified latitude
     * and longitude coordinates.
     */
    fun fromCoordinates(coordinates: Coordinates, zoom: Int): TileCoordinates {
      val zoomFactor = 1 shl zoom
      val latRad = coordinates.lat.toRadians()
      val x1 = zoomFactor * (coordinates.lng + 180) / 360
      val y1 = zoomFactor * (1 - (ln(tan(latRad) + sec(latRad)) / Math.PI)) / 2
      val (x, y) = PixelCoordinates((x1 * 256.0).toInt(), (y1 * 256.0).toInt(), zoom)
      return TileCoordinates(x / 256, y / 256, zoom)
    }

    /**
     * Returns all tiles at a particular zoom contained within the specified latitude and longitude
     * bounds.
     */
    fun withinBounds(bounds: Bounds, zoom: Int): List<TileCoordinates> {
      val results = mutableListOf<TileCoordinates>()
      val nwTile = fromCoordinates(bounds.northwest, zoom)
      val seTile = fromCoordinates(bounds.southeast, zoom)
      for (y in nwTile.y..seTile.y) {
        for (x in nwTile.x..seTile.x) {
          results.add(TileCoordinates(x, y, zoom))
        }
      }
      return results.toList()
    }
  }
}
