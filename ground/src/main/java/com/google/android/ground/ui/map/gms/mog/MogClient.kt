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
import com.google.android.gms.maps.model.Tile
import java.io.FileNotFoundException
import java.io.InputStream
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import mogtest.*
import timber.log.Timber

private fun LongRange.immediatelyPrecedes(other: LongRange): Boolean = this.last + 1 == other.first

private fun LongRange.merge(other: LongRange): LongRange = LongRange(this.first, other.last)

/** Client responsible for fetching MOG metadata and image tiles. */
class MogClient(val collection: MogCollection) {

  private val cache: LruCache<String, Deferred<MogMetadata?>> = LruCache(16)
  /** Keyed by the bounds of the containing MOG. */
  private val tileIndexCache: LruCache<TileCoordinates, Deferred<MogTileIndex>> =
    LruCache(16 * 50 * 50)

  @Suppress("MemberVisibilityCanBePrivate")
  suspend fun getMogMetadata(bounds: TileCoordinates): MogMetadata? =
    getMogMetadata(collection.getMogUrl(bounds), bounds)

  //  fun getTiles(mogTileSetRequests: List<MogTileSetRequest>): Flow<Pair<TileCoordinates, Tile>> =
  //    flow {
  //      mogTileSetRequests.forEach { emitAll(getTiles(it)) }
  //    }

  /**
   * Convenience method which requests and returns the tile with the specified tile coordinates, or
   * `null` if not available.
   */
  suspend fun getTile(tileCoordinates: TileCoordinates): Tile? {
    val (x, y, zoom) = tileCoordinates
    val mogMetadata = getMogMetadataForTile(tileCoordinates) ?: return null
    val mogImageMetadata = mogMetadata.getImageMetadata(zoom) ?: return null
    val mogImageTileIndex = getMogTileIndex(mogMetadata, zoom) ?: return null
    val byteRange = mogImageTileIndex.getByteRange(x, y) ?: return null
    val tileMetadata = MogTileMetadata(mogImageMetadata, tileCoordinates, byteRange)
    val (_, tile) = getTiles(mogMetadata.url, listOf(tileMetadata)).firstOrNull() ?: return null
    return tile
  }

  /**
   * Returns the byte ranges of tiles overlapping the specified [bounds] and [zoomRange]s, fetching
   * required metadata if not already in cache.
   *
   * @param bounds the bounds used to constrain which tiles are retrieved. Only tiles within or
   *   overlapping these bounds are retrieved.
   * @param zoomRange the min. and max. zoom levels for which tiles should be retrieved. Defaults to
   *   all available tiles in the collection as determined by the [MogCollection.hiResMogMaxZoom].
   */
  suspend fun getTilesRequests(
    bounds: LatLngBounds,
    zoomRange: IntRange = 0..collection.hiResMogMaxZoom
  ): List<MogTilesRequest> {
    val hiResMogMinZoom = collection.hiResMogMinZoom
    val requests = mutableListOf<MogTilesRequest>()
    val (loResZoomLevels, hiResZoomLevels) = zoomRange.partition { it < hiResMogMinZoom }
    if (loResZoomLevels.isNotEmpty()) {
      requests.addAll(getTileRequestsForPyramid(TileCoordinates.WORLD, bounds, loResZoomLevels))
    }
    if (hiResZoomLevels.isNotEmpty()) {
      // Compute extents of first and last region covered by specified bounds.
      val nwMogBounds = TileCoordinates.fromLatLng(bounds.northwest(), hiResMogMinZoom)
      val seMogBounds = TileCoordinates.fromLatLng(bounds.southeast(), hiResMogMinZoom)
      for (y in nwMogBounds.y..seMogBounds.y) {
        for (x in nwMogBounds.x..seMogBounds.x) {
          val mogBounds = TileCoordinates(x, y, hiResMogMinZoom)
          requests.addAll(getTileRequestsForPyramid(mogBounds, bounds, hiResZoomLevels))
        }
      }
    }
    return requests
  }

  private suspend fun getMogMetadataForTile(tileCoordinates: TileCoordinates): MogMetadata? =
    getMogMetadata(collection.getMogBoundsForTile(tileCoordinates))



  private suspend fun getTileRequestsForPyramid(
    mogBounds: TileCoordinates,
    tileBounds: LatLngBounds,
    zoomLevels: List<Int>
  ): List<MogTilesRequest> {
    val mogMetadata = getMogMetadata(mogBounds) ?: return listOf()
    val tiles = zoomLevels.flatMap { zoom -> tileMetdataAtZoom(mogMetadata, tileBounds, zoom) }
    // Group tiles by proximity in source file.
    val tilesRequests = mutableListOf<MutableTileByteRange>()
    for (tile in tiles) {
      val (imageMetadata, tileCoordinates, byteRange) = tile
      val (x, y, _) = tileCoordinates
      val prev = tilesRequests.lastOrNull()
      if (prev == null || byteRange.first - prev.byteRange.last - 1 > MAX_OVER_FETCH_PER_TILE) {
        tilesRequests.add(MutableTileByteRange(byteRange, mutableListOf(tileCoordinates)))
      } else {
        prev.extendRange(byteRange.last, tileCoordinates)
      }
    }
    return tilesRequests.map { it.toTilesRequest() }
  }

  private suspend fun tileMetdataAtZoom(
    mogMetadata: MogMetadata,
    tileBounds: LatLngBounds,
    zoom: Int
  ): List<MogTileMetadata> {
    val mogImageMetadata = mogMetadata.getImageMetadata(zoom) ?: return listOf()
    val mogTileIndex = getMogTileIndex(mogMetadata, zoom) ?: return listOf()
    TileCoordinates.withinBounds(tileBounds, zoom).mapNotNull { it ->
      mogTileIndex.getByteRange(it.x, it.y)?.let {
        MogTileMetadata(mogImageMetadata, mogMetadata.bounds, it)
      }
    }
  }

  private suspend fun getMogTileIndex(mogMetadata: MogMetadata, zoom: Int): MogTileIndex? {
    val mogImageMetadata = mogMetadata.getImageMetadata(zoom) ?: return null
    // TODO: Refactor into MogMetadataReader
    val tileOffsetsByteRange = mogImageMetadata.tileOffsetsByteRange
    val byteCountsByteRange = mogImageMetadata.byteCountsByteRange
    if (!tileOffsetsByteRange.immediatelyPrecedes(byteCountsByteRange)) {
      Timber.d("Ignoring IFD with non-contiguous tileOffset and byteCount entries")
      return null
    }
    UrlInputStream(mogMetadata.url, tileOffsetsByteRange.merge(byteCountsByteRange)).use {
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
      return MogTileIndex(mogImageMetadata, tileOffsets, byteCounts)
    }
  }

  private fun getTileByteRanges(
    mogTileIndex: MogTileIndex,
    tileCoordinatesList: List<TileCoordinates>
  ): List<MogTileByteRange> {
    val tilesRequests = mutableListOf<MutableTileByteRange>()
    for (tileCoordinates in tileCoordinatesList) {
      val (x, y, _) = tileCoordinates
      val byteRange = mogTileIndex.getByteRange(x, y) ?: continue
      val prev = tilesRequests.lastOrNull()
      if (prev == null || byteRange.first - prev.byteRange.last - 1 > MAX_OVER_FETCH_PER_TILE) {
        tilesRequests.add(MutableTileByteRange(byteRange, mutableListOf(tileCoordinates)))
      } else {
        prev.extendRange(byteRange.last, tileCoordinates)
      }
    }
    return tilesRequests.map { it.toTilesRequest() }
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
    @Suppress("DeferredResultUnused")
    async { nullIfNotFound { UrlInputStream(url) }?.use { readMetadata(url, mogBounds, it) } }
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
      val tagValues = MogMetadataReader.readMetadata(inputStream)
      val ifds = mutableListOf<MogImageMetadata>()
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
        ifds.add(
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
      return MogMetadata(url, bounds, ifds.toList())
    } catch (e: Throwable) {
      error("Failed to read $url: ${e.message})")
    } finally {
      inputStream.close()
    }
  }

  // TODO: Use thread pool to request multiple ranges in parallel.
  /**
   * Returns a [Flow] which emits the tiles for the specified tile coordinates along with each
   * tile's respective coordinates.
   */
  fun getTiles(url: String, tiles: List<MogTileMetadata>): Flow<Pair<TileCoordinates, Tile>> =
    flow {
      val byteRange = TileByteRange(tiles.first().byteRange.first, tiles.last().byteRange.last)
      UrlInputStream(url, byteRange).use { emitAll(readTiles(tiles, it)) }
    }

  private fun readTiles(
    tiles: List<MogTileMetadata>,
    inputStream: InputStream
  ): Flow<Pair<TileCoordinates, Tile>> = flow {
    val tileReader = MogTileReader(inputStream)
    for (tileMetadata in tiles) {
      val tile = tileReader.readTile(tileMetadata)
      emit(Pair(tileMetadata.tileCoordinates, tile))
    }
  }
}

private inline fun <T> nullIfNotFound(fn: () -> T) =
  try {
    fn()
  } catch (_: FileNotFoundException) {
    null
  }
