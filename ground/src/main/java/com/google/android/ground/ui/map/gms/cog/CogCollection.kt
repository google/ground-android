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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.net.URL
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import timber.log.Timber

val WORLD = TileCoordinates(0, 0, 0)

fun LatLngBounds.northwest() = LatLng(northeast.latitude, southwest.longitude)

fun LatLngBounds.southeast() = LatLng(southwest.latitude, northeast.longitude)

/**
 * Represents a collection of non-overlapping cloud-optimized GeoTIFFs (COGs) whose extents are
 * determined by the boundaries of web mercator tiles at [tileSetExtentsZoom].
 */
class CogCollection(
  private val cogProvider: CogProvider,
  private val urlTemplate: String,
  private val worldImageUrl: String,
  private val tileSetExtentsZoom: Int,
  private val cache: LruCache<String, Deferred<Cog>> = LruCache(16)
) {

  private fun TileCoordinates.getUrl() =
    urlTemplate
      .replace("{x}", x.toString())
      .replace("{y}", y.toString())
      .replace("{z}", zoom.toString())

  /** Returns the COG containing the tile with the specified coordinates. */
  private fun getCogForTile(tile: TileCoordinates): Cog {
    // TODO: Consider replacing runBlocking/async with synchronized(url) to simplify impl and error
    // handling.
    return if (tile.zoom < tileSetExtentsZoom) {
      runBlocking { getOrFetchCogAsync(worldImageUrl, WORLD).await() }
    } else {
      val extent = tile.originAtZoom(tileSetExtentsZoom)
      runBlocking { getOrFetchCogAsync(extent.getUrl(), extent).await() }
    }
  }

  /**
   * Returns the future containing the COG with the specified extent and URL. The COG is loaded from
   * in-memory cache if present, otherwise it's asynchronously fetched from remote. The process of
   * checking in cache and creating the new job is synchronized on the current instance of
   * CogCollection to prevent duplicate requests due to race conditions.
   */
  private fun getOrFetchCogAsync(url: String, extent: TileCoordinates): Deferred<Cog> =
    synchronized(this) { cache.get(url) ?: fetchCogAsync(url, extent) }

  /**
   * Asynchronously fetches and returns the COG with the specified extent and URL. The async job is
   * added to the cache immediately to prevent duplicate fetches from other threads.
   */
  private fun fetchCogAsync(url: String, extent: TileCoordinates): Deferred<Cog> = runBlocking {
    // TODO: Exceptions get propagated as cancellation of the coroutine. Handle them!
    @Suppress("DeferredResultUnused")
    async { cogProvider.getCog(URL(url), extent) }.also { cache.put(url, it) }
  }

  /** Returns the specified tile, or `null` if unavailable. */
  fun getTile(tile: TileCoordinates): CogTile? =
    getCogForTile(tile).imagesByZoomLevel[tile.zoom]?.getTile(tile)

  fun getTiles(bounds: LatLngBounds, zoomLevels: IntRange): Flow<Result<CogTile>> = flow {
    // TODO: Handle zoomLevels < tileSetExtentsZoom using world COG.
    // Compute extents of first and last COG covered by specified bounds.
    val nwCog = TileCoordinates.fromLatLng(bounds.northwest(), tileSetExtentsZoom)
    val seCog = TileCoordinates.fromLatLng(bounds.southeast(), tileSetExtentsZoom)
    for (y in nwCog.y..seCog.y) {
      for (x in nwCog.x..seCog.x) {
        val cogExtents = TileCoordinates(x, y, tileSetExtentsZoom)
        try {
          // TODO: Add method to get COG by extents.
          val cog = getCogForTile(cogExtents)
          for (z in zoomLevels) {
            val image = cog.imagesByZoomLevel[z]
            if (image != null) emitAll(image.getTiles(bounds))
          }
        } catch (e: Throwable) {
          Timber.d(e, "Error fetching COG $cogExtents")
        }
      }
    }
  }
}
