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
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.TiffReader
import mil.nga.tiff.util.TiffConstants
import mil.nga.tiff.util.TiffException
import timber.log.Timber

/** Client responsible for fetching MOG headers and image tiles. */
class MogClient(val collection: MogCollection) {
  private val cache: LruCache<String, Deferred<MogMetadata?>> = LruCache(16)

  @Suppress("MemberVisibilityCanBePrivate")
  suspend fun getMogMetadata(bounds: TileCoordinates): MogMetadata? =
    getMogMetadata(collection.getMogUrl(bounds), bounds)

  /**
   * Returns a [Flow] which emits the tiles for the specified tile coordinates along with each
   * tile's respective coordinates.
   */
  fun getTiles(tilesRequests: List<TilesRequest>): Flow<Pair<TileCoordinates, Tile>> = flow {
    tilesRequests.forEach { emitAll(getTiles(it)) }
  }

  /**
   * Convenience method which requests and returns the tile with the specified tile coordinates, or
   * `null` if not available.
   */
  suspend fun getTile(tileCoordinates: TileCoordinates): Tile? {
    val metadata = getMogMetadataForTile(tileCoordinates) ?: return null
    val byteRange = metadata.getByteRange(tileCoordinates) ?: return null
    val request = TilesRequest(metadata, byteRange, listOf(tileCoordinates))
    return getTiles(request).firstOrNull()?.second
  }

  /**
   * Builds requests for tiles overlapping the specified bounds at the specified zoomRanges.
   *
   * @param bounds the bounds used to constrain which tiles are retrieved. Only tiles within or
   *   overlapping these bounds are retrieved.
   * @param zoomRange the min. and max. zoom levels for which tiles should be retrieved. Defaults to
   *   all available tiles in the collection as determined by the [MogCollection.hiResMogMaxZoom].
   */
  suspend fun buildTilesRequests(
    bounds: LatLngBounds,
    zoomRange: IntRange = 0..collection.hiResMogMaxZoom
  ): List<TilesRequest> {
    val hiResMogMinZoom = collection.hiResMogMinZoom
    val requests = mutableListOf<TilesRequest>()
    val (loResZoomLevels, hiResZoomLevels) = zoomRange.partition { it < hiResMogMinZoom }
    if (loResZoomLevels.isNotEmpty()) {
      requests.addAll(buildTilesRequests(TileCoordinates.WORLD, bounds, loResZoomLevels))
    }
    if (hiResZoomLevels.isNotEmpty()) {
      // Compute extents of first and last region covered by specified bounds.
      val nwMogBounds = TileCoordinates.fromLatLng(bounds.northwest(), hiResMogMinZoom)
      val seMogBounds = TileCoordinates.fromLatLng(bounds.southeast(), hiResMogMinZoom)
      for (y in nwMogBounds.y..seMogBounds.y) {
        for (x in nwMogBounds.x..seMogBounds.x) {
          val mogBounds = TileCoordinates(x, y, hiResMogMinZoom)
          requests.addAll(buildTilesRequests(mogBounds, bounds, hiResZoomLevels))
        }
      }
    }
    return requests
  }

  private suspend fun getMogMetadataForTile(tileCoordinates: TileCoordinates): MogMetadata? =
    getMogMetadata(collection.getMogBoundsForTile(tileCoordinates))

  private suspend fun buildTilesRequests(
    mogBounds: TileCoordinates,
    tileBounds: LatLngBounds,
    zoomLevels: List<Int>
  ): List<TilesRequest> {
    val mogMetadata = getMogMetadata(mogBounds) ?: return listOf()
    return zoomLevels.flatMap { zoom ->
      buildTilesRequests(mogMetadata, TileCoordinates.withinBounds(tileBounds, zoom))
    }
  }

  private fun buildTilesRequests(
    mogMetadata: MogMetadata,
    tileCoordinatesList: List<TileCoordinates>
  ): List<TilesRequest> {
    val tilesRequests = mutableListOf<MutableTilesRequest>()
    for (tileCoordinates in tileCoordinatesList) {
      val byteRange = mogMetadata.getByteRange(tileCoordinates) ?: continue
      val prev = tilesRequests.lastOrNull()
      if (prev == null || byteRange.first - prev.byteRange.last - 1 > MAX_OVER_FETCH_PER_TILE) {
        tilesRequests.add(
          MutableTilesRequest(mogMetadata, byteRange, mutableListOf(tileCoordinates))
        )
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
    async { nullIfNotFound { UrlInputStream(url) }?.use { readHeader(url, mogBounds, it) } }
      .also { cache.put(url, it) }
  }

  private fun readHeader(
    url: String,
    extent: TileCoordinates,
    inputStream: InputStream
  ): MogMetadata {
    val startTimeMillis = System.currentTimeMillis()
    try {
      // This reads only headers and not the whole file.
      val tiff = TiffReader.readTiff(inputStream)
      val images = mutableListOf<MogImageMetadata>()
      // Only include image file directories with RGB image data. Mask images are skipped.
      // TODO: Render masked areas as transparent.
      val rgbIfds =
        tiff.fileDirectories.filter {
          it
            .getIntegerEntryValue(FieldTagType.PhotometricInterpretation)
            .and(TiffConstants.PHOTOMETRIC_INTERPRETATION_RGB) != 0
        }
      // IFDs are in decreasing detail (decreasing zoom), starting with max, ending with min zoom.
      val maxZ = extent.zoom + rgbIfds.size - 1
      rgbIfds.forEachIndexed { i, ifd ->
        images.add(
          MogImageMetadata(
            ifd.getIntegerEntryValue(FieldTagType.TileWidth),
            ifd.getIntegerEntryValue(FieldTagType.TileLength),
            extent.originAtZoom(maxZ - i),
            ifd.getLongListEntryValue(FieldTagType.TileOffsets),
            ifd.getLongListEntryValue(FieldTagType.TileByteCounts),
            ifd.getIntegerEntryValue(FieldTagType.ImageWidth),
            ifd.getIntegerEntryValue(FieldTagType.ImageLength),
            ifd.getLongListEntryValue(FieldTagType.JPEGTables)?.map(Long::toByte)?.toByteArray()
              ?: byteArrayOf()
          )
        )
      }
      val time = System.currentTimeMillis() - startTimeMillis
      Timber.d("Loaded headers from $url in $time ms")

      return MogMetadata(url, extent, images.toList())
    } catch (e: TiffException) {
      error("Failed to read $url: ${e.message})")
    } finally {
      inputStream.close()
    }
  }

  // TODO: Use thread pool to request multiple ranges in parallel.
  fun getTiles(tilesRequest: TilesRequest): Flow<Pair<TileCoordinates, Tile>> = flow {
    UrlInputStream(tilesRequest.mogMetadata.url, tilesRequest.byteRange).use {
      emitAll(readTiles(tilesRequest, it))
    }
  }

  private fun readTiles(
    tilesRequest: TilesRequest,
    inputStream: InputStream
  ): Flow<Pair<TileCoordinates, Tile>> = flow {
    var pos: Long? = null
    for (tileCoordinates in tilesRequest.tileCoordinatesList) {
      val image = tilesRequest.mogMetadata.getImageMetadata(tileCoordinates.zoom)!!
      val byteRange =
        image.getByteRange(tileCoordinates.x, tileCoordinates.y)
          ?: error("$tileCoordinates out of image bounds")
      if (pos != null && pos < byteRange.first) {
        while (pos++ < byteRange.first) {
          if (inputStream.read() == -1) {
            error("Unexpected end of tile response")
          }
        }
      }
      val startTimeMillis = System.currentTimeMillis()
      val imageBytes = image.parseTile(inputStream, byteRange.count())
      val time = System.currentTimeMillis() - startTimeMillis
      Timber.d("Fetched tile ${tileCoordinates}: ${imageBytes.size} in $time ms")
      emit(Pair(tileCoordinates, Tile(image.tileWidth, image.tileLength, imageBytes)))
      pos = byteRange.last + 1
    }
  }
}

private inline fun <T> nullIfNotFound(fn: () -> T) =
  try {
    fn()
  } catch (_: FileNotFoundException) {
    null
  }
