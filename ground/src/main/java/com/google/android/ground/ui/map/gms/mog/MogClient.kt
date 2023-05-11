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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.TiffReader
import mil.nga.tiff.util.TiffConstants
import mil.nga.tiff.util.TiffException
import timber.log.Timber

/** Client responsible for fetching MOG headers and image tiles. */
class MogClient {
  private val cache: LruCache<String, Deferred<MogMetadata?>> = LruCache(16)
  /** Returns the tile for the specified tile coordinates, or `null` if not available. */
  suspend fun getTile(collection: MogCollection, tileCoordinates: TileCoordinates): Tile? =
    getMogMetadataForTile(collection, tileCoordinates)?.getTile(tileCoordinates)

  suspend fun getTilesRequests(
    collection: MogCollection,
    bounds: LatLngBounds,
    zoomRange: IntRange = 0..collection.hiResMogMaxZoom
  ): List<TilesRequest> {
    val hiResMogMinZoom = collection.hiResMogMinZoom
    val requests = mutableListOf<TilesRequest>()
    val (worldZoomLevels, sliceZoomLevels) = zoomRange.partition { it < hiResMogMinZoom }
    if (worldZoomLevels.isNotEmpty()) {
      requests.addAll(getTilesRequests(collection, TileCoordinates.WORLD, bounds, worldZoomLevels))
    }
    if (sliceZoomLevels.isNotEmpty()) {
      // Compute extents of first and last region covered by specified bounds.
      val nwMogBounds = TileCoordinates.fromLatLng(bounds.northwest(), hiResMogMinZoom)
      val seMogBounds = TileCoordinates.fromLatLng(bounds.southeast(), hiResMogMinZoom)
      for (y in nwMogBounds.y..seMogBounds.y) {
        for (x in nwMogBounds.x..seMogBounds.x) {
          val mogExtent = TileCoordinates(x, y, hiResMogMinZoom)
          requests.addAll(getTilesRequests(collection, mogExtent, bounds, sliceZoomLevels))
        }
      }
    }
    return requests
  }

  private suspend fun getMogMetadataForTile(
    collection: MogCollection,
    tileCoordinates: TileCoordinates
  ): MogMetadata? = getMogMetadata(collection, collection.getMogBoundsForTile(tileCoordinates))

  private suspend fun getTilesRequests(
    collection: MogCollection,
    mogBounds: TileCoordinates,
    tileBounds: LatLngBounds,
    zoomLevels: List<Int>
  ): List<TilesRequest> {
    val mog = getMogMetadata(collection, mogBounds) ?: return listOf()
    return zoomLevels.flatMap { zoom ->
      mog.getTilesRequests(TileCoordinates.withinBounds(tileBounds, zoom))
    }
  }

  private suspend fun getMogMetadata(
    collection: MogCollection,
    bounds: TileCoordinates
  ): MogMetadata? = getMogMetadata(collection.getMogUrl(bounds), bounds)

  /**
   * Returns a [Flow] which emits the tiles for the specified tile coordinates along with each
   * tile's respective coordinates.
   */
  fun fetchTiles(tilesRequests: List<TilesRequest>): Flow<Pair<TileCoordinates, Tile>> = flow {
    tilesRequests.forEach { request ->
      val mog = getMogMetadata(request.mogUrl, request.mogBounds) ?: return@flow
      emitAll(mog.fetchTiles(request))
    }
  }

  /** Returns metadata for the MOG bounded by the specified web mercator tile. */
  private suspend fun getMogMetadata(url: String, bounds: TileCoordinates): MogMetadata? =
    getOrFetchMogAsync(url, bounds).await()

  /**
   * Returns a future containing the [MogMetadata] with the specified extent and URL. Metadata is
   * either loaded from in-memory cache if present, or asynchronously fetched if necessary. The
   * process of checking the cache and creating the new job is synchronized on the current instance
   * to prevent duplicate parallel requests for the same resource.
   */
  private fun getOrFetchMogAsync(url: String, extent: TileCoordinates): Deferred<MogMetadata?> =
    synchronized(this) { cache.get(url) ?: fetchMogAsync(url, extent) }

  /**
   * Asynchronously fetches and returns the [MogMetadata] with the specified extent and URL. The
   * async job is added to the cache immediately to prevent duplicate fetches from other threads.
   */
  private fun fetchMogAsync(url: String, extent: TileCoordinates): Deferred<MogMetadata?> =
    runBlocking {
      // TODO: Exceptions get propagated as cancellation of the coroutine. Handle them!
      @Suppress("DeferredResultUnused")
      async { nullIfNotFound { UrlInputStream(url) }?.use { readHeader(url, extent, it) } }
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
}

private inline fun <T> nullIfNotFound(fn: () -> T) =
  try {
    fn()
  } catch (_: FileNotFoundException) {
    null
  }
