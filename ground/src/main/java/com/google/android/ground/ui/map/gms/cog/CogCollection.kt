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

import android.util.LruCache
import java.net.URL

class CogCollection(
  private val cogProvider: CogProvider,
  private val urlTemplate: String,
  private val tileSetExtentsZ: Int
) {
  private val cache = LruCache<String, Cog>(32)

  private fun getTileSetUrl(extent: TileCoordinates) =
    URL(
      urlTemplate
        .replace("{x}", extent.x.toString())
        .replace("{y}", extent.y.toString())
        .replace("{z}", extent.zoom.toString())
    )

  fun getCog(tile: TileCoordinates): Cog? {
    if (tile.zoom < tileSetExtentsZ) return null
    val extent = tile.originAtZoom(tileSetExtentsZ)
    val url = getTileSetUrl(extent)
    var cog = cache.get(url.toString())
    if (cog != null) return cog
    // TODO: Block on loading of headers from same file.
    cog = cogProvider.getCog(url, extent)
    cache.put(url.toString(), cog)
    //    val cogFile = File(url)
    //    if (!cogFile.exists()) return null
    // TODO: Cache headers instead of fetching every time.
    return cog
  }

  /** Returns the tile for the specified coordinates, or `null` if unavailable. */
  fun getTile(coordinates: TileCoordinates): CogTile? {
    val cog = getCog(coordinates) ?: return null
    val image = cog.imagesByZoomLevel[coordinates.zoom] ?: return null
    return image.getTile(coordinates)
  }
}
