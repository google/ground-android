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

import android.util.LruCache
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.ui.map.gms.mog.TileCoordinates.Companion.WORLD
import java.io.FileNotFoundException
import java.io.InputStream
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/** Client responsible for fetching and caching MOG metadata and image tiles. */
class MogClient(val collection: MogCollection) {

  private val cache: LruCache<String, Deferred<MogMetadata?>> = LruCache(16)

  /** Returns the tile with the specified coordinates, or `null` if not available. */
  suspend fun getTile(tileCoordinates: TileCoordinates): MogTile? {
    val mogMetadata = getMogMetadataForTile(tileCoordinates) ?: return null
    val tileMetadata = getTileMetadata(mogMetadata, tileCoordinates) ?: return null
    val requests = getTileRequests(mogMetadata.sourceUrl, listOf(tileMetadata))
    return getTiles(requests).first()
  }

  /** Returns the metadata for the MOG with the specified bounds. */
  @Suppress("MemberVisibilityCanBePrivate")
  suspend fun getMogMetadata(mogBounds: TileCoordinates): MogMetadata? =
    getMogMetadata(collection.getMogUrl(mogBounds), mogBounds)

  /**
   * Returns the byte ranges of tiles overlapping the specified [tileBounds] and [zoomRange]s,
   * fetching required metadata if not already in cache.
   *
   * @param tileBounds the bounds used to constrain which tiles are retrieved. Only tiles within or
   * overlapping these bounds are retrieved.
   * @param zoomRange the min. and max. zoom levels for which tiles should be retrieved. Defaults to
   * all available tiles in the collection as determined by the [MogCollection.hiResMogMaxZoom].
   */
  suspend fun getTilesRequests(
    tileBounds: LatLngBounds,
    zoomRange: IntRange = 0..collection.hiResMogMaxZoom
  ): List<MogTilesRequest> {
    val hiResMogMinZoom = collection.hiResMogMinZoom
    val requests = mutableListOf<MogTilesRequest>()
    val (loResZoomLevels, hiResZoomLevels) = zoomRange.partition { it < hiResMogMinZoom }
    if (loResZoomLevels.isNotEmpty()) {
      requests.addAll(getTileRequestsForPyramid(WORLD, tileBounds, loResZoomLevels))
    }
    if (hiResZoomLevels.isNotEmpty()) {
      // Compute tile coordinates of first and last MOG covered by specified bounds.
      val nwMogBounds = TileCoordinates.fromLatLng(tileBounds.northwest(), hiResMogMinZoom)
      val seMogBounds = TileCoordinates.fromLatLng(tileBounds.southeast(), hiResMogMinZoom)
      for (y in nwMogBounds.y..seMogBounds.y) {
        for (x in nwMogBounds.x..seMogBounds.x) {
          val mogBounds = TileCoordinates(x, y, hiResMogMinZoom)
          requests.addAll(getTileRequestsForPyramid(mogBounds, tileBounds, hiResZoomLevels))
        }
      }
    }
    return requests
  }

  /**
   * Returns a [Flow] which emits the tiles for the specified tile coordinates along with each
   * tile's respective coordinates.
   */
  fun getTiles(requests: List<MogTilesRequest>): Flow<MogTile> = flow {
    // TODO(#1704): Use thread pool to request multiple ranges in parallel.
    requests.forEach { emitAll(getTiles(it)) }
  }

  /**
   * Returns a [Flow] which emits the tiles for the specified tile coordinates along with each
   * tile's respective coordinates.
   */
  // TODO(#1596): Cache the jpeg files locally instead of downloading each time.
  private fun getTiles(tilesRequest: MogTilesRequest): Flow<MogTile> = flow {
    UrlInputStream(tilesRequest.sourceUrl, tilesRequest.byteRange).use { inputStream ->
      emitAll(
        MogTileReader(inputStream, tilesRequest.byteRange.first).readTiles(tilesRequest.tiles)
      )
    }
  }

  /**
   * Returns the metadata for the MOG which contains the tile with the specified coordinate, or
   * `null` if unavailable.
   */
  private suspend fun getMogMetadataForTile(tileCoordinates: TileCoordinates): MogMetadata? =
    getMogMetadata(collection.getMogBoundsForTile(tileCoordinates))

  /**
   * Builds and returns the tile requests which can be used to fetch all available tiles within
   * [tileBounds] at [zoomLevels] within the MOG with the specified [mogBounds]. Consecutive byte
   * ranges are consolidated to minimized the number of individual requests required.
   */
  private suspend fun getTileRequestsForPyramid(
    mogCoordinates: TileCoordinates,
    tileBounds: LatLngBounds,
    zoomLevels: List<Int>
  ): List<MogTilesRequest> {
    val mogMetadata = getMogMetadata(mogCoordinates) ?: return listOf()
    val tiles = zoomLevels.flatMap { zoom -> getTileMetadata(mogMetadata, tileBounds, zoom) }
    return getTileRequests(mogMetadata.sourceUrl, tiles)
  }

  /**
   * Builds and returns the tile requests which can be used to fetch the specified tiles.
   * Consecutive byte ranges are consolidated to minimized the number of individual requests
   * required.
   */
  private fun getTileRequests(
    sourceUrl: String,
    tiles: List<MogTileMetadata>
  ): List<MogTilesRequest> {
    val tilesRequests = mutableListOf<MutableMogTilesRequest>()
    for (tile in tiles) {
      // Create a new request for the first tile and for each non adjacent tile.
      val lastOffset = tilesRequests.lastOrNull()?.tiles?.last()?.byteRange?.last
      if (lastOffset == null || tile.byteRange.first - lastOffset - 1 > MAX_OVER_FETCH_PER_TILE) {
        tilesRequests.add(MutableMogTilesRequest(sourceUrl))
      }
      tilesRequests.last().appendTile(tile)
    }
    return tilesRequests.map { it.toTilesRequest() }
  }

  /**
   * Returns the metadata for all tiles provided by the specified MOG within the provided bounds.
   */
  private fun getTileMetadata(
    mogMetadata: MogMetadata,
    tileBounds: LatLngBounds,
    zoom: Int
  ): List<MogTileMetadata> =
    TileCoordinates.withinBounds(tileBounds, zoom).mapNotNull { tileCoordinates ->
      getTileMetadata(mogMetadata, tileCoordinates)
    }

  /**
   * Returns the metadata for the tiles with the specified coordinates provided by the specified
   * MOG.
   */
  private fun getTileMetadata(
    mogMetadata: MogMetadata,
    tileCoordinates: TileCoordinates
  ): MogTileMetadata? {
    val imageMetadata = mogMetadata.getImageMetadata(tileCoordinates.zoom) ?: return null
    val byteRange = imageMetadata.getByteRange(tileCoordinates.x, tileCoordinates.y) ?: return null
    return MogTileMetadata(
      tileCoordinates,
      imageMetadata.tileWidth,
      imageMetadata.tileLength,
      imageMetadata.jpegTables,
      byteRange
    )
  }

  /** Returns metadata for the MOG bounded by the specified web mercator tile. */
  private suspend fun getMogMetadata(url: String, bounds: TileCoordinates): MogMetadata? =
    getMogMetadataAsync(url, bounds).await()

  /**
   * Returns a future containing the [MogMetadata] with the specified extent and URL. Metadata is
   * either loaded from in-memory cache if present, or asynchronously fetched if necessary. The
   * process of checking the cache and creating the new job is synchronized on the current instance
   * to prevent duplicate parallel requests for the same resource.
   */
  private fun getMogMetadataAsync(url: String, mogBounds: TileCoordinates): Deferred<MogMetadata?> =
    synchronized(this) { cache.get(url) ?: getMogMetadataFromRemoteAsync(url, mogBounds) }

  /**
   * Asynchronously fetches and returns the [MogMetadata] with the specified extent and URL. The
   * async job is added to the cache immediately to prevent duplicate fetches from other threads.
   */
  private fun getMogMetadataFromRemoteAsync(
    url: String,
    mogCoordinates: TileCoordinates
  ): Deferred<MogMetadata?> = runBlocking {
    // TODO: Exceptions get propagated as cancellation of the coroutine. Handle them!
    async {
        nullIfNotFound { UrlInputStream(url) }?.use { readMogMetadata(url, mogCoordinates, it) }
      }
      .also { cache.put(url, it) }
  }

  /**
   * Reads the metadata from the specified input stream. The stream is not closed upon completion or
   * on error.
   */
  private fun readMogMetadata(
    sourceUrl: String,
    mogBounds: TileCoordinates,
    inputStream: InputStream
  ): MogMetadata {
    val startTimeMillis = System.currentTimeMillis()
    inputStream.use { inputStreamAutoCloseable ->
      // Read the MOG headers (not the whole file).
      val reader = MogMetadataReader(SeekableInputStream(inputStreamAutoCloseable))
      val ifds = reader.readImageFileDirectories()
      val imageMetadata = mutableListOf<MogImageMetadata>()
      // IFDs are in decreasing detail (decreasing zoom), starting with max, ending with min zoom.
      val maxZ = mogBounds.zoom + ifds.size - 1
      ifds.forEachIndexed { i, entry ->
        imageMetadata.add(
          MogImageMetadata.fromTiffTags(
            originTile = mogBounds.originAtZoom(maxZ - i),
            tiffTagToValue = entry
          )
        )
      }
      val time = System.currentTimeMillis() - startTimeMillis
      Timber.d("Read headers from $sourceUrl in $time ms")
      return MogMetadata(sourceUrl, mogBounds, imageMetadata.toList())
    }
  }
}

private inline fun <T> nullIfNotFound(fn: () -> T) =
  try {
    fn()
  } catch (_: FileNotFoundException) {
    null
  }
