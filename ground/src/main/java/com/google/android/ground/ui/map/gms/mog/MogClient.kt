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
import mogtest.*
import timber.log.Timber

/** Client responsible for fetching MOG metadata and image tiles. */
class MogClient(val collection: MogCollection) {

  private val cache: LruCache<String, Deferred<MogMetadata?>> = LruCache(16)
  /** Keyed by the MOG's source URL and the image's zoom level. */
  private val tileIndexCache: LruCache<Pair<String, Int>, MogTileIndex> = LruCache(16 * 50 * 50)

  @Suppress("MemberVisibilityCanBePrivate")
  suspend fun getMogMetadata(bounds: TileCoordinates): MogMetadata? =
    getMogMetadata(collection.getMogUrl(bounds), bounds)

  //  fun getTiles(mogTileSetRequests: List<MogTileSetRequest>): Flow<Pair<TileCoordinates, Tile>> =
  //    flow {
  //      mogTileSetRequests.forEach { emitAll(getTiles(it)) }
  //    }

  /**
   * Convenience function which requests and returns the tile with the specified tile coordinates,
   * or `null` if not available.
   */
  //  suspend fun getTile(tileCoordinates: TileCoordinates): MogTile? {
  //    val (x, y, zoom) = tileCoordinates
  //    val mogMetadata = getMogMetadataForTile(tileCoordinates) ?: return null
  //    val imageMetadata = mogMetadata.getImageMetadata(zoom) ?: return null
  //    val tileIndex = getTileIndex(mogMetadata.sourceUrl, imageMetadata, zoom) ?: return null
  //    val byteRange = tileIndex.getByteRange(x, y) ?: return null
  //    val tileMetadata =
  //      MogTileMetadata(
  //        tileCoordinates,
  //        imageMetadata.tileWidth,
  //        imageMetadata.tileLength,
  //        imageMetadata.jpegTables,
  //        byteRange
  //      )
  //    val tilesRequest = MogTilesRequest(mogMetadata.sourceUrl, listOf(tileMetadata))
  //    return getTiles(tilesRequest).firstOrNull()
  //  }

  /**
   * Returns the byte ranges of tiles overlapping the specified [tileBounds] and [zoomRange]s,
   * fetching required metadata if not already in cache.
   *
   * @param tileBounds the bounds used to constrain which tiles are retrieved. Only tiles within or
   *   overlapping these bounds are retrieved.
   * @param zoomRange the min. and max. zoom levels for which tiles should be retrieved. Defaults to
   *   all available tiles in the collection as determined by the [MogCollection.hiResMogMaxZoom].
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
      // Compute extents of first and last region covered by specified bounds.
      val nwMogCoordinates = TileCoordinates.fromLatLng(tileBounds.northwest(), hiResMogMinZoom)
      val seMogCoordinates = TileCoordinates.fromLatLng(tileBounds.southeast(), hiResMogMinZoom)
      for (y in nwMogCoordinates.y..seMogCoordinates.y) {
        for (x in nwMogCoordinates.x..seMogCoordinates.x) {
          val mogCoordinates = TileCoordinates(x, y, hiResMogMinZoom)
          requests.addAll(getTileRequestsForPyramid(mogCoordinates, tileBounds, hiResZoomLevels))
        }
      }
    }
    return requests
  }

  // TODO: Use thread pool to request multiple ranges in parallel.
  /**
   * Returns a [Flow] which emits the tiles for the specified tile coordinates along with each
   * tile's respective coordinates.
   */
  fun getTiles(requests: List<MogTilesRequest>): Flow<MogTile> = flow {
    requests.forEach { emitAll(getTiles(it)) }
  }

  /**
   * Returns a [Flow] which emits the tiles for the specified tile coordinates along with each
   * tile's respective coordinates.
   */
  private fun getTiles(tilesRequest: MogTilesRequest): Flow<MogTile> = flow {
    UrlInputStream(tilesRequest.sourceUrl, tilesRequest.byteRange).use { inputStream ->
      emitAll(MogTileReader(inputStream).readTiles(tilesRequest.tiles))
    }
  }
  //  private suspend fun getMogMetadataForTile(tileCoordinates: TileCoordinates): MogMetadata? =
  //    getMogMetadata(collection.getMogCoordinatesForTile(tileCoordinates))

  //  suspend fun getImageMetadataForTile(tileCoordinates: TileCoordinates) =
  //    getMogMetadataForTile(tileCoordinates)?.getImageMetadata(tileCoordinates.zoom)

  private suspend fun getTileRequestsForPyramid(
    mogCoordinates: TileCoordinates,
    tileBounds: LatLngBounds,
    zoomLevels: List<Int>
  ): List<MogTilesRequest> {
    val mogMetadata = getMogMetadata(mogCoordinates) ?: return listOf()
    val tiles = zoomLevels.flatMap { zoom -> tileMetadataAtZoom(mogMetadata, tileBounds, zoom) }
    return getTileRequests(mogMetadata, tiles)
  }

  private fun getTileRequests(
    mogMetadata: MogMetadata,
    tiles: List<MogTileMetadata>
  ): List<MogTilesRequest> {
    val tilesRequests = mutableListOf<MutableMogTilesRequest>()
    for (tile in tiles) {
      // Create a new request for the first tile and for each non adjacent tile.
      val lastOffset = tilesRequests.lastOrNull()?.tiles?.last()?.byteRange?.last
      if (lastOffset == null || tile.byteRange.first - lastOffset - 1 > MAX_OVER_FETCH_PER_TILE) {
        tilesRequests.add(MutableMogTilesRequest(mogMetadata.sourceUrl))
      }
      tilesRequests.last().appendTile(tile)
    }
    return tilesRequests.map { it.toTilesRequest() }
  }

  private suspend fun tileMetadataAtZoom(
    mogMetadata: MogMetadata,
    tileBounds: LatLngBounds,
    zoom: Int
  ): List<MogTileMetadata> =
    TileCoordinates.withinBounds(tileBounds, zoom).mapNotNull { tileCoordinates ->
      getTileMetadata(mogMetadata, tileCoordinates)
    }

  private fun getTileMetadata(
    mogMetadata: MogMetadata,
    tileCoordinates: TileCoordinates
  ): MogTileMetadata? {
    val mogTileIndex = getTileIndex(mogMetadata, tileCoordinates.zoom) ?: return null
    val imageMetadata = mogTileIndex.imageMetadata
    val byteRange = mogTileIndex.getByteRange(tileCoordinates.x, tileCoordinates.y) ?: return null
    return MogTileMetadata(
      tileCoordinates,
      imageMetadata.tileWidth,
      imageMetadata.tileLength,
      imageMetadata.jpegTables,
      byteRange
    )
  }

  private fun getTileIndex(mogMetadata: MogMetadata, zoom: Int): MogTileIndex? {
    val tileIndexCacheKey = Pair(mogMetadata.sourceUrl, zoom)
    var tileIndex = tileIndexCache.get(tileIndexCacheKey)
    if (tileIndex != null) return tileIndex

    val imageMetadata = mogMetadata.getImageMetadata(zoom) ?: return null
    val tileOffsetsByteRange = imageMetadata.tileOffsetsByteRange
    val byteCountsByteRange = imageMetadata.byteCountsByteRange
    if (!tileOffsetsByteRange.immediatelyPrecedes(byteCountsByteRange)) {
      Timber.d("Ignoring IFD with non-contiguous tileOffset and byteCount entries")
      return null
    }
    val requestByteRange = tileOffsetsByteRange.merge(byteCountsByteRange)
    UrlInputStream(mogMetadata.sourceUrl, requestByteRange).use {
      // TODO: Refactor into MogMetadataReader
      val bytes = IOUtils.streamBytes(it)
      val reader = ByteReader(bytes)
      val tileOffsets =
        MogMetadataReader.readValues(
          reader,
          TiffTagDataType.LONG,
          tileOffsetsByteRange.count().toLong()
        ) as List<Long>
      val byteCounts =
        MogMetadataReader.readValues(
          reader,
          TiffTagDataType.LONG,
          byteCountsByteRange.count().toLong()
        ) as List<Long>
      tileIndex = MogTileIndex(imageMetadata, tileOffsets, byteCounts)
      // TODO: Make tileIndex Deferred to prevent dupe requests.
      tileIndexCache.put(tileIndexCacheKey, tileIndex)
      return tileIndex
    }
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
  private fun getMogMetadataAsync(
    url: String,
    mogCoordinates: TileCoordinates
  ): Deferred<MogMetadata?> =
    synchronized(this) { cache.get(url) ?: getMogMetadataFromRemoteAsync(url, mogCoordinates) }

  /**
   * Asynchronously fetches and returns the [MogMetadata] with the specified extent and URL. The
   * async job is added to the cache immediately to prevent duplicate fetches from other threads.
   */
  private fun getMogMetadataFromRemoteAsync(
    url: String,
    mogCoordinates: TileCoordinates
  ): Deferred<MogMetadata?> = runBlocking {
    // TODO: Exceptions get propagated as cancellation of the coroutine. Handle them!
    @Suppress("DeferredResultUnused")
    async { nullIfNotFound { UrlInputStream(url) }?.use { readMetadata(url, mogCoordinates, it) } }
      .also { cache.put(url, it) }
  }

  private fun readMetadata(
    url: String,
    bounds: TileCoordinates,
    inputStream: InputStream
  ): MogMetadata {
    val startTimeMillis = System.currentTimeMillis()
    try {
      // This reads only headers and not the whole file.
      val tagValues = MogMetadataReader.readImageFileDirectories(inputStream)
      val imageMetadata = mutableListOf<MogImageMetadata>()
      // Only include image file directories with RGB image data. Mask images are skipped.
      // TODO: Render masked areas as transparent.
      val rgbIfds =
        tagValues.filter {
          (it[TiffTag.PhotometricInterpretation] as Int).and(
            TiffConstants.PHOTOMETRIC_INTERPRETATION_RGB
          ) != 0
        }
      // IFDs are in decreasing detail (decreasing zoom), starting with max, ending with min zoom.
      val maxZ = bounds.zoom + rgbIfds.size - 1
      rgbIfds.forEachIndexed { i, entry ->
        imageMetadata.add(
          MogImageMetadata(
            entry[TiffTag.TileWidth] as Int,
            entry[TiffTag.TileLength] as Int,
            bounds.originAtZoom(maxZ - i),
            entry[TiffTag.TileOffsets] as LongRange,
            entry[TiffTag.TileByteCounts] as LongRange,
            entry[TiffTag.ImageWidth] as Int,
            entry[TiffTag.ImageLength] as Int,
            (entry[TiffTag.JPEGTables] as List<*>?)?.map { (it as Long).toByte() }?.toByteArray()
              ?: byteArrayOf()
          )
        )
      }
      val time = System.currentTimeMillis() - startTimeMillis
      Timber.d("Loaded header from $url in $time ms")
      return MogMetadata(url, bounds, imageMetadata.toList())
    } catch (e: Throwable) {
      error("Failed to read $url: ${e.message})")
    } finally {
      inputStream.close()
    }
  }
}

private inline fun <T> nullIfNotFound(fn: () -> T) =
  try {
    fn()
  } catch (_: FileNotFoundException) {
    null
  }

private fun LongRange.immediatelyPrecedes(other: LongRange): Boolean = this.last + 1 == other.first

private fun LongRange.merge(other: LongRange): LongRange = LongRange(this.first, other.last)
