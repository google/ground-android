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

package com.google.android.ground.ui.map.gms.tcog

import android.util.LruCache
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

val WORLD = TileCoordinates(0, 0, 0)

fun LatLngBounds.northwest() = LatLng(northeast.latitude, southwest.longitude)

fun LatLngBounds.southeast() = LatLng(southwest.latitude, northeast.longitude)

/**
 * A collection of tiled cloud-optimized GeoTIFFs (COGs). See `README.md` for full description of
 * supported format and usage.
 */
class TiledCogCollection(
  private val cogHeaderParser: CogHeaderParser,
  private val cogSource: CogSource,
  private val worldImageUrl: String,
  private val sliceUrlTemplate: String,
  private val sliceMinZoom: Int,
  val maxZoom: Int
) {
  private val cache: LruCache<String, Deferred<Cog?>> = LruCache(16)

  private fun getCogExtentsForTile(tileCoordinates: TileCoordinates): TileCoordinates =
    if (tileCoordinates.zoom < sliceMinZoom) WORLD else tileCoordinates.originAtZoom(sliceMinZoom)

  /** Returns the COG containing the tile with the specified coordinates. */
  private suspend fun getCogForTile(tileCoordinates: TileCoordinates): Cog? =
    getCog(getCogExtentsForTile(tileCoordinates))

  private suspend fun getCog(extent: TileCoordinates): Cog? =
    getOrFetchCogAsync(getCogUrl(extent), extent).await()

  private fun getCogUrl(extent: TileCoordinates): String {
    val (x, y, zoom) = extent
    if (zoom == 0) {
      return worldImageUrl
    }
    if (zoom < sliceMinZoom) {
      error("Invalid zoom for this collection. Expected 0 or $sliceMinZoom, got $zoom")
    }
    return sliceUrlTemplate
      .replace("{x}", x.toString())
      .replace("{y}", y.toString())
      .replace("{z}", zoom.toString())
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
    // TODO: Exceptions get propagated as cancellation of the coroutine. Handle them!
    @Suppress("DeferredResultUnused")
    val deferred = async {
      cogSource.openStream(url)?.use { cogHeaderParser.getCog(url, extent, it) }
    }
    cache.put(url, deferred)
  }

  suspend fun getTile(tile: TileCoordinates): CogTile? =
    getCogForTile(tile)?.getTile(cogSource, tile)

  fun getTiles(bounds: LatLngBounds, zoomRange: IntRange): Flow<CogTile> = flow {
    val (worldZoomLevels, sliceZoomLevels) = zoomRange.partition { it < sliceMinZoom }
    if (worldZoomLevels.isNotEmpty()) {
      emitAll(getTiles(WORLD, bounds, worldZoomLevels))
    }
    if (sliceZoomLevels.isNotEmpty()) {
      // Compute extents of first and last COG slice covered by specified bounds.
      val nwSlice = TileCoordinates.fromLatLng(bounds.northwest(), sliceMinZoom)
      val seSlice = TileCoordinates.fromLatLng(bounds.southeast(), sliceMinZoom)
      for (y in nwSlice.y..seSlice.y) {
        for (x in nwSlice.x..seSlice.x) {
          emitAll(getTiles(TileCoordinates(x, y, sliceMinZoom), bounds, sliceZoomLevels))
        }
      }
    }
  }
  private fun getTiles(
    cogCoordinates: TileCoordinates,
    bounds: LatLngBounds,
    zoomLevels: List<Int>
  ): Flow<CogTile> = flow {
    val cog = getCog(cogCoordinates) ?: return@flow
    for (zoom in zoomLevels) {
      emitAll(cog.getTiles(cogSource, TileCoordinates.withinBounds(bounds, zoom)))
    }
  }
}
