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

package org.groundplatform.android.ui.map.gms.mog

import android.util.LruCache
import com.google.firebase.storage.StorageException
import java.io.InputStream
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.groundplatform.android.data.remote.RemoteStorageManager
import org.groundplatform.android.model.map.Bounds
import timber.log.Timber

/** Aliases a relative path or a URL to a MOG. */
typealias MogPathOrUrl = String

/** Aliases a fetch-able URL to a MOG. */
typealias MogUrl = String

/** Client responsible for fetching and caching MOG metadata and image tiles. */
class MogClient(
  val collection: MogCollection,
  val remoteStorageManager: RemoteStorageManager,
  private val inputStreamFactory: (String, LongRange?) -> InputStream = { url, range ->
    UrlInputStream(url, range)
  },
) {

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
   *   tiles within or overlapping these bounds are returned.
   * @param zoomRange the min. and max. zoom levels for which tile requests should be returned.
   *   Defaults to all available zoom levels in the collection ([MogSource.minZoom] to
   *   [MogSource.maxZoom]).
   */
  suspend fun buildTilesRequests(
    tileBounds: Bounds,
    zoomRange: IntRange = collection.sources.zoomRange(),
  ) =
    zoomRange
      .flatMap { zoom -> buildTileRequests(tileBounds, zoom) }
      .consolidate(MAX_OVER_FETCH_PER_TILE)

  /** Returns requests for tiles in the specified bounds and zoom, one request per tile. */
  private suspend fun buildTileRequests(tileBounds: Bounds, zoom: Int): List<MogTilesRequest> {
    val mogSource = collection.getMogSource(zoom) ?: return listOf()
    return TileCoordinates.withinBounds(tileBounds, zoom).mapNotNull {
      buildTileRequest(mogSource, it)
    }
  }

  /** Returns a request for the specified tile. */
  private suspend fun buildTileRequest(
    mogSource: MogSource,
    tileCoordinates: TileCoordinates,
  ): MogTilesRequest? {
    val mogBounds = mogSource.getMogBoundsForTile(tileCoordinates)
    val mogPath = mogSource.getMogPath(mogBounds)
    val mogMetadata = getMogMetadata(mogPath, mogBounds)
    val tileMetadata = mogMetadata?.let { getTileMetadata(it, tileCoordinates) }
    return tileMetadata?.let { MogTilesRequest(mogMetadata.sourceUrl, listOf(it)) }
  }

  /**
   * Returns a [Flow] which emits the tiles for the specified tile coordinates along with each
   * tile's respective coordinates.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  fun getTiles(requests: List<MogTilesRequest>): Flow<MogTile> = flow {
    // TODO: Use thread pool to request multiple ranges in parallel.
    // Issue URL: https://github.com/google/ground-android/issues/1704
    val results = mutableListOf<Deferred<Flow<MogTile>>>()
    withContext(Dispatchers.IO.limitedParallelism(200)) {
      for (request in requests) {
        val result = async { getTiles(request) }
        results.add(result)
      }
    }

    // Wait for all requests to complete
    val finalResult = results.awaitAll()

    for (result in finalResult) {
      emitAll(result)
    }
  }

  /**
   * Returns a [Flow] which emits the tiles for the specified tile coordinates along with each
   * tile's respective coordinates.
   */
  private fun getTiles(tilesRequest: MogTilesRequest): Flow<MogTile> = flow {
    inputStreamFactory(tilesRequest.sourceUrl, tilesRequest.byteRange).use { inputStream ->
      emitAll(
        MogTileReader(inputStream, tilesRequest.byteRange.first).readTiles(tilesRequest.tiles)
      )
    }
  }

  /**
   * Returns the metadata for the MOG which contains the tile with the specified coordinates, or
   * `null` if unavailable.
   */
  private suspend fun getMogMetadataForTile(tileCoordinates: TileCoordinates): MogMetadata? {
    val mogSource = collection.getMogSource(tileCoordinates.zoom) ?: return null
    val mogBounds = mogSource.getMogBoundsForTile(tileCoordinates)
    val mogPath = mogSource.getMogPath(mogBounds)
    return getMogMetadata(mogPath, mogBounds)
  }

  /**
   * Returns the metadata for the tiles with the specified coordinates provided by the specified
   * MOG.
   */
  private fun getTileMetadata(
    mogMetadata: MogMetadata,
    tileCoordinates: TileCoordinates,
  ): MogTileMetadata? {
    val imageMetadata = mogMetadata.getImageMetadata(tileCoordinates.zoom) ?: return null
    val byteRange = imageMetadata.getByteRange(tileCoordinates.x, tileCoordinates.y) ?: return null
    return MogTileMetadata(
      tileCoordinates,
      imageMetadata.tileWidth,
      imageMetadata.tileLength,
      imageMetadata.jpegTables,
      byteRange,
      imageMetadata.noDataValue,
    )
  }

  /** Returns metadata for the MOG bounded by the specified web mercator tile. */
  private suspend fun getMogMetadata(path: String, bounds: TileCoordinates): MogMetadata? =
    getMogMetadataAsync(path, bounds).await()

  /**
   * Returns a future containing the [MogMetadata] with the specified extent and path. Metadata is
   * either loaded from in-memory cache if present, or asynchronously fetched if necessary. The
   * process of checking the cache and creating the new job is synchronized on the current instance
   * to prevent duplicate parallel requests for the same resource.
   */
  private fun getMogMetadataAsync(
    path: MogPathOrUrl,
    mogBounds: TileCoordinates,
  ): Deferred<MogMetadata?> =
    synchronized(this) { cache.get(path) ?: getMogMetadataFromRemoteAsync(path, mogBounds) }

  /**
   * Asynchronously fetches and returns the [MogMetadata] with the specified extent and path. The
   * async job is added to the cache immediately to prevent duplicate fetches from other threads.
   */
  private fun getMogMetadataFromRemoteAsync(
    path: MogPathOrUrl,
    mogBounds: TileCoordinates,
  ): Deferred<MogMetadata?> = runBlocking {
    // TODO: Exceptions get propagated as cancellation of the coroutine. Handle them!
    // Issue URL: https://github.com/google/ground-android/issues/2903
    async { path.toUrl()?.readMetadata(mogBounds) }.also { cache.put(path, it) }
  }

  private fun readMogMetadata(
    sourceUrl: String,
    mogBounds: TileCoordinates,
    inputStream: InputStream,
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
          tiffTagToValue = entry,
        )
      )
    }
    return MogMetadata(sourceUrl, mogBounds, imageMetadata.toList())
  }

  private suspend fun MogPathOrUrl.toUrl(): MogUrl? =
    if (startsWith("/")) {
      try {
        remoteStorageManager.getDownloadUrl(this).toString()
      } catch (e: StorageException) {
        Timber.w(e, "File not found for path: $this")
        null
      }
    } else this

  private fun MogUrl.readMetadata(mogBounds: TileCoordinates): MogMetadata =
    inputStreamFactory(this, null).use { this.readMogMetadataAndClose(mogBounds, it) }

  /** Reads the metadata from the specified input stream. */
  private fun MogUrl.readMogMetadataAndClose(
    mogBounds: TileCoordinates,
    inputStream: InputStream,
  ): MogMetadata {
    val startTimeMillis = System.currentTimeMillis()
    return inputStream
      .use { readMogMetadata(this, mogBounds, it) }
      .apply {
        val elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis
        Timber.d("Read headers from $sourceUrl in $elapsedTimeMillis ms")
      }
  }
}
