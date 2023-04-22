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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * Represents a collection of non-overlapping cloud-optimized GeoTIFFs (COGs) whose extents are
 * determined by the boundaries of web mercator tiles at [tileSetExtentsZoom].
 */
class CogCollection(
  private val cogProvider: CogProvider,
  private val urlTemplate: String,
  private val tileSetExtentsZoom: Int
) {
  private val cache = LruCache<String, Deferred<Cog?>>(16)

  private fun TileCoordinates.getUrl() =
    urlTemplate
      .replace("{x}", x.toString())
      .replace("{y}", y.toString())
      .replace("{z}", zoom.toString())

  /** Returns the COG containing the tile with the specified coordinates. */
  private fun getCogForTile(tile: TileCoordinates): Cog? {
    if (tile.zoom < tileSetExtentsZoom) return null
    val extent = tile.originAtZoom(tileSetExtentsZoom)
    return runBlocking { getOrFetchCogAsync(extent.getUrl(), tile).await() }
  }

  /**
   * Returns the future containing the COG with the specified extent and URL. The COG is loaded from
   * in-memory cache if present, otherwise it's asynchronously fetched from remote. The process of
   * checking in cache and creating the new job is synchronized on the current instance of
   * CogCollection to prevent duplicate requests due to race conditions.
   */
  private fun getOrFetchCogAsync(url: String, extent: TileCoordinates): Deferred<Cog?> =
    synchronized(this) { cache.get(url) ?: fetchCogAsync(url, extent) }

  /**
   * Asynchronously fetches and returns the COG with the specified extent and URL. The async job is
   * added to the cache immediately to prevent duplicate fetches from other threads.
   */
  private fun fetchCogAsync(url: String, extent: TileCoordinates): Deferred<Cog?> = runBlocking {
    @Suppress("DeferredResultUnused")
    async { cogProvider.getCog(URL(url), extent) }.also { cache.put(url, it) }
  }

  /** Returns the specified tile, or `null` if unavailable. */
  fun getTile(tile: TileCoordinates): CogTile? =
    getCogForTile(tile)?.imagesByZoomLevel?.get(tile.zoom)?.getTile(tile)
}
