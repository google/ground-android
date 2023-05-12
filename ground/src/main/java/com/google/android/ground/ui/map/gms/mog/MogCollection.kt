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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.Tile
import java.io.ByteArrayOutputStream

/** A collection of Maps Optimized GeoTIFFs (MOGs). */
@Suppress("MemberVisibilityCanBePrivate")
class MogCollection(
  val worldMogUrl: String,
  val hiResMogUrl: String,
  val hiResMogMinZoom: Int,
  val hiResMogMaxZoom: Int
) {
  fun getMogUrl(bounds: TileCoordinates): String {
    val (x, y, zoom) = bounds
    if (zoom == 0) {
      return worldMogUrl.replace("{z}", hiResMogMinZoom.toString())
    }
    if (zoom < hiResMogMinZoom) {
      error("Invalid zoom for this collection. Expected 0 or $hiResMogMinZoom, got $zoom")
    }
    return hiResMogUrl
      .replace("{x}", x.toString())
      .replace("{y}", y.toString())
      .replace("{z}", hiResMogMinZoom.toString())
  }

  /**
   * Crude method of making missing pixels transparent. Ideally, rather than replacing dark pixels
   * with transparent ones, we would use the image masks contained.
   */
  fun applyMask(tile: Tile?, tileCoordinates: TileCoordinates): Tile? {
    // Only apply mask workaround to world COG for now.
    if (tile?.data == null || tileCoordinates.zoom >= hiResMogMinZoom) {
      return tile
    }
    val bitmap =
      BitmapFactory.decodeByteArray(tile.data, 0, tile.data!!.size)
        .copy(Bitmap.Config.ARGB_8888, true)
    bitmap.setHasAlpha(true)
    for (x in 0 until bitmap.width) {
      for (y in 0 until bitmap.height) {
        val color = bitmap.getPixel(x, y)
        val r: Int = color shr 16 and 0xFF
        val g: Int = color shr 8 and 0xFF
        val b: Int = color shr 0 and 0xFF
        if (r + g + b == 0) {
          bitmap.setPixel(x, y, 0)
        }
      }
    }
    val out = ByteArrayOutputStream()
    // Note: JPEG doesn't support transparency, so need to use PNG or BMP.
    // TODO: Return raw BMP instead of recompressing to JPEG.
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    return Tile(tile.width, tile.height, out.toByteArray())
  }

  /** Returns the bounds of the MOG containing the tile with the specified coordinates. */
  fun getMogBoundsForTile(tileCoordinates: TileCoordinates): TileCoordinates =
    if (tileCoordinates.zoom < hiResMogMinZoom) TileCoordinates.WORLD
    else tileCoordinates.originAtZoom(hiResMogMinZoom)
}
