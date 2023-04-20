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

package com.google.android.ground.ui.map.gms.cog

import java.io.File

class CogCollection(
  private val cogProvider: CogProvider,
  private val urlTemplate: String,
  private val tileSetExtentsZ: Int
) {

  private fun getTileSetUrl(extent: TileCoordinates) =
    urlTemplate
      .replace("{x}", extent.x.toString())
      .replace("{y}", extent.y.toString())
      .replace("{z}", extent.z.toString())

  fun getCog(tile: TileCoordinates): Cog? {
    if (tile.z < tileSetExtentsZ) return null
    val extent = tile.originAtZoom(tileSetExtentsZ)
    val url = getTileSetUrl(extent)
    val cogFile = File(url)
    if (!cogFile.exists()) return null
    // TODO: Cache headers instead of fetching every time.
    return cogProvider.getCog(cogFile, extent)
  }

  /** Returns the tile for the specified coordinates, or `null` if unavailable. */
  fun getTile(coordinates: TileCoordinates): CogTile? {
    val cog = getCog(coordinates) ?: return null
    val image = cog.imagesByZoomLevel[coordinates.z] ?: return null
    return image.getTile(coordinates)
  }
}
