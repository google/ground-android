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
    val tileRequest = MogTilesRequest(mogMetadata.sourceUrl, listOf(tileMetadata))
    return getTiles(tileRequest).firstOrNull()
  }

  /**
   * Fetches metadata and builds the minimal set of requests required to fetch tiles overlapping the
   * specified [tileBounds] and [zoomRange]s.
   *
   * @param tileBounds the bounds used to constrain which tiles are retrieved. Only requests for
   * tiles within or overlapping these bounds are returned.
   * @param zoomRange the min. and max. zoom levels for which tile requests should be returned.
   * Defaults to all available zoom levels in the collection ([MogCollection.minZoom] to
   * [MogCollection.maxZoom]).
   */
  suspend fun buildTilesRequests(
    tileBounds: LatLngBounds,
    zoomRange: IntRange = IntRange(collection.minZoom, collection.maxZoom)
  ) =
    zoomRange
      .flatMap { zoom -> buildTileRequests(tileBounds, zoom) }
      .consolidate(MAX_OVER_FETCH_PER_TILE)

  /** Returns requests for tiles in the specified bounds and zoom, one request per tile. */
  private suspend fun buildTileRequests(
    tileBounds: LatLngBounds,
    zoom: Int
  ): List<MogTilesRequest> {
    val mogSource = collection.getMogSource(zoom) ?: return listOf()
    return TileCoordinates.withinBounds(tileBounds, zoom).mapNotNull {
      buildTileRequest(mogSource, it)
    }
  }

  /** Returns a request for the specified tile. */
  private suspend fun buildTileRequest(
    mogSource: MogSource,
    tileCoordinates: TileCoordinates
  ): MogTilesRequest? {
    val mogBounds = mogSource.getMogBoundsForTile(tileCoordinates)
    val mogUrl = mogSource.getMogUrl(mogBounds)
    val mogMetadata = getMogMetadata(mogUrl, mogBounds) ?: return null
    val tileMetadata = getTileMetadata(mogMetadata, tileCoordinates) ?: return null
    return MogTilesRequest(mogUrl, listOf(tileMetadata))
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
  private suspend fun getMogMetadataForTile(tileCoordinates: TileCoordinates): MogMetadata? {
    val mogSource = collection.getMogSource(tileCoordinates.zoom) ?: return null
    val mogBounds = mogSource.getMogBoundsForTile(tileCoordinates)
    val mogUrl = mogSource.getMogUrl(mogBounds)
    return getMogMetadata(mogUrl, mogBounds)
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
    mogBounds: TileCoordinates
  ): Deferred<MogMetadata?> = runBlocking {
    // TODO: Exceptions get propagated as cancellation of the coroutine. Handle them!
    async {
        nullIfNotFound { UrlInputStream(url) }?.use { readMogMetadataAndClose(url, mogBounds, it) }
      }
      .also { cache.put(url, it) }
  }

  /** Reads the metadata from the specified input stream. */
  private fun readMogMetadataAndClose(
    sourceUrl: String,
    mogBounds: TileCoordinates,
    inputStream: InputStream
  ): MogMetadata {
    val startTimeMillis = System.currentTimeMillis()
    return inputStream
      .use { readMogMetadata(sourceUrl, mogBounds, it) }
      .apply {
        val elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis
        Timber.d("Read headers from $sourceUrl in $elapsedTimeMillis ms")
      }
  }

  private fun readMogMetadata(
    sourceUrl: String,
    mogBounds: TileCoordinates,
    inputStream: InputStream
  ): MogMetadata {
    // Read the MOG headers (not the whole file).
    val reader = MogMetadataReader(SeekableInputStream(inputStream))
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
    return MogMetadata(sourceUrl, mogBounds, imageMetadata.toList())
  }
}

private inline fun <T> nullIfNotFound(fn: () -> T) =
  try {
    fn()
  } catch (_: FileNotFoundException) {
    null
  }
