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

package com.google.android.ground.ui.map.gms

import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.TileProvider.NO_TILE
import com.google.android.ground.ui.map.gms.mog.ImageEditor
import com.google.android.ground.ui.map.gms.mog.TileCoordinates
import com.google.android.ground.ui.map.gms.mog.toPixelBounds
import com.google.android.ground.ui.map.gms.mog.toPixelCoordinate

private const val MAX_ZOOM = 19

class ClippingTileProvider(
  private val sourceTileProvider: TileProvider,
  clipBounds: List<LatLngBounds>,
) : TileProvider {

  private val pixelBounds = clipBounds.map { it.toPixelBounds(MAX_ZOOM) }

  override fun getTile(x: Int, y: Int, zoom: Int): Tile {
    val sourceTile = sourceTileProvider.getTile(x, y, zoom) ?: NO_TILE
    if (sourceTile == NO_TILE) return sourceTile
    // We assume if a tile is returned by the source provider that at least some pixels are within
    // the clip bounds, so there's no need to optimize by checking before clipping.
    return clipToBounds(TileCoordinates(x, y, zoom), sourceTile)
  }

  private fun clipToBounds(tileCoords: TileCoordinates, tile: Tile): Tile {
    if (tile.data == null) return NO_TILE
    val output =
      ImageEditor.setTransparentIf(tile.data!!) { _, x, y ->
        val pixelCoords = tileCoords.toPixelCoordinate(x, y)
        pixelBounds.none { it.contains(pixelCoords) }
      }
    return Tile(tile.width, tile.height, output)
  }
}
