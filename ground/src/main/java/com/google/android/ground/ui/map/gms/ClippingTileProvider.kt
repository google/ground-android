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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.TileProvider.NO_TILE
import com.google.android.ground.ui.map.gms.mog.TileCoordinates
import com.google.android.ground.ui.map.gms.mog.toPixelBounds
import com.google.android.ground.ui.map.gms.mog.toPixelCoordinate
import java.io.ByteArrayOutputStream

private const val MAX_ZOOM = 19

class ClippingTileProvider(
  private val sourceTileProvider: TileProvider,
  clipBounds: List<LatLngBounds>
) : TileProvider {

  private val pixelBounds = clipBounds.map { it.toPixelBounds(MAX_ZOOM) }

  override fun getTile(x: Int, y: Int, zoom: Int): Tile {
    val sourceTile = sourceTileProvider.getTile(x, y, zoom) ?: NO_TILE
    if (sourceTile == NO_TILE) return sourceTile
    // TODO: Optimization: return NO_TILE immediately if we known tile is completely out of clip
    // bounds.
    return clipToBounds(TileCoordinates(x, y, zoom), sourceTile)
  }

  private fun clipToBounds(tileCoords: TileCoordinates, tile: Tile): Tile {
    if (tile.data == null) return NO_TILE
    val opts = BitmapFactory.Options()
    opts.inMutable = true
    val bitmap = BitmapFactory.decodeByteArray(tile.data, 0, tile.data!!.size, opts)
    bitmap.setHasAlpha(true)
    for (y in 0 until bitmap.height) {
      for (x in 0 until bitmap.width) {
        val pixelCoords = tileCoords.toPixelCoordinate(x, y)
        if (pixelBounds.none { it.contains(pixelCoords) }) {
          bitmap.setPixel(x, y, Color.TRANSPARENT)
        }
      }
    }
    // Android doesn't implement encoders for uncompressed format, so we must compress the returned
    // tile so that it can be later encoded by Maps SDK. Experimentally, decompressing JPG and
    // compressing WEBP each tile adds on the order of 1ms to each tile which we can consider
    // negligible.
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream)
    return Tile(tile.width, tile.height, stream.toByteArray())
  }
}
