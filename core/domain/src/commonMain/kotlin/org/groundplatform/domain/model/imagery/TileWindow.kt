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
package org.groundplatform.domain.model.imagery

import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.map.Bounds

/**
 * A square window onto web-mercator imagery, framing a target region: which zoom to draw at, which
 * tiles cover the window, and where any coordinate lands inside it.
 *
 * @property zoom Web-mercator zoom level the window is drawn at.
 * @property sizePx Size of the window (square) in pixels.
 * @property originX World-pixel x of the window's top-left corner at [zoom].
 * @property originY World-pixel y of the window's top-left corner at [zoom].
 */
data class TileWindow(val zoom: Int, val sizePx: Int, val originX: Int, val originY: Int) {

  /** An offset in pixels from the top-left corner of a [TileWindow]. */
  data class PixelOffset(val x: Int, val y: Int)

  data class Placement(val tile: TileCoordinates, val offset: PixelOffset, val sizePx: Int)

  val bounds: Bounds
    get() =
      Bounds(
        southwest =
          PixelCoordinates(originX, originY + this@TileWindow.sizePx, zoom).toCoordinates(),
        northeast =
          PixelCoordinates(originX + this@TileWindow.sizePx, originY, zoom).toCoordinates(),
      )

  fun tilesAt(sourceZoom: Int): List<Placement> {
    require(sourceZoom <= zoom) { "Source zoom $sourceZoom is deeper than the window's zoom $zoom" }
    // Every tile at one zoom is the same size, so this is settled once for the whole batch.
    val tileSidePx = 256 shl (zoom - sourceZoom)
    val minTileX = originX.floorDiv(tileSidePx)
    val maxTileX = (originX + sizePx - 1).floorDiv(tileSidePx)
    val minTileY = originY.floorDiv(tileSidePx)
    val maxTileY = (originY + sizePx - 1).floorDiv(tileSidePx)
    return buildList {
      for (tileY in minTileY..maxTileY) {
        for (tileX in minTileX..maxTileX) {
          add(
            Placement(
              tile = TileCoordinates(tileX, tileY, sourceZoom),
              offset = PixelOffset(tileX * tileSidePx - originX, tileY * tileSidePx - originY),
              sizePx = tileSidePx,
            )
          )
        }
      }
    }
  }

  /**
   * Pixel distance from this window's top-left corner to [coordinates]. Values can be negative or
   * exceed the window size when the point is outside the visible area.
   */
  fun offsetOf(coordinates: Coordinates): PixelOffset {
    val pixel = coordinates.toPixelCoordinates(zoom)
    return PixelOffset(pixel.x - originX, pixel.y - originY)
  }

  companion object {
    fun fitTo(target: Bounds, sizePx: Int, maxZoom: Int, paddingPx: Int = 0): TileWindow {
      val usablePx = (sizePx - 2 * paddingPx).coerceAtLeast(1)
      // Return the highest zoom level whose image still fits in the available space.
      val zoom = (0..maxZoom.coerceAtLeast(0)).lastOrNull { target.fitsIn(usablePx, it) } ?: 0
      val northwest = target.northwest.toPixelCoordinates(zoom)
      val southeast = target.southeast.toPixelCoordinates(zoom)
      val centerX = (northwest.x + southeast.x) / 2
      val centerY = (northwest.y + southeast.y) / 2
      return TileWindow(
        zoom = zoom,
        sizePx = sizePx,
        originX = centerX - sizePx / 2,
        originY = centerY - sizePx / 2,
      )
    }

    private fun Bounds.fitsIn(usablePx: Int, zoom: Int): Boolean {
      val northwest = northwest.toPixelCoordinates(zoom)
      val southeast = southeast.toPixelCoordinates(zoom)
      return southeast.x - northwest.x <= usablePx && southeast.y - northwest.y <= usablePx
    }
  }
}
