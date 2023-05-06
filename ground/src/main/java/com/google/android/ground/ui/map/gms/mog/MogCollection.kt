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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
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

fun LatLngBounds.northwest() = LatLng(northeast.latitude, southwest.longitude)

fun LatLngBounds.southeast() = LatLng(southwest.latitude, northeast.longitude)

inline fun <T> nullIfNotFound(fn: () -> T) =
  try {
    fn()
  } catch (_: FileNotFoundException) {
    null
  }

/** A collection of cloud-optimized GeoTIFFs (COGs). */
class MogCollection(
  private val worldCogUrl: String,
  private val cellCogUrlTemplate: String,
  private val cellCogMinZoom: Int,
  val cellCogMaxZoom: Int
) : TileProvider {
  private val cache: LruCache<String, Deferred<Mog?>> = LruCache(16)

  private fun getCogExtentsForTile(tileCoordinates: TileCoordinates): TileCoordinates =
    if (tileCoordinates.zoom < cellCogMinZoom) TileCoordinates.WORLD
    else tileCoordinates.originAtZoom(cellCogMinZoom)

  /** Returns the COG containing the tile with the specified coordinates. */
  private suspend fun getCogForTile(tileCoordinates: TileCoordinates): Mog? =
    getCog(getCogExtentsForTile(tileCoordinates))

  private suspend fun getCog(extent: TileCoordinates): Mog? =
    getOrFetchCogAsync(getCogUrl(extent), extent).await()

  private fun getCogUrl(extent: TileCoordinates): String {
    val (x, y, zoom) = extent
    if (zoom == 0) {
      return worldCogUrl
    }
    if (zoom < cellCogMinZoom) {
      error("Invalid zoom for this collection. Expected 0 or $cellCogMinZoom, got $zoom")
    }
    return cellCogUrlTemplate
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
  private fun getOrFetchCogAsync(url: String, extent: TileCoordinates): Deferred<Mog?> =
    synchronized(this) { cache.get(url) ?: fetchCogAsync(url, extent) }

  /**
   * Asynchronously fetches and returns the COG with the specified extent and URL. The async job is
   * added to the cache immediately to prevent duplicate fetches from other threads.
   */
  private fun fetchCogAsync(url: String, extent: TileCoordinates): Deferred<Mog?> =
    runBlocking {
      // TODO: Exceptions get propagated as cancellation of the coroutine. Handle them!
      @Suppress("DeferredResultUnused")
      async { nullIfNotFound { UrlInputStream(url) }?.use { readHeader(url, extent, it) } }
        .also { cache.put(url, it) }
    }

  override fun getTile(x: Int, y: Int, zoom: Int): Tile? = runBlocking {
    val tileCoordinates = TileCoordinates(x, y, zoom)
    try {
      getCogForTile(tileCoordinates)?.getTile(tileCoordinates)
    } catch (e: Throwable) {
      // We must catch and log errors ourselves since Maps SDK doesn't do this for us.
      Timber.d(e, "Error fetching tile at $tileCoordinates")
      null
    }
  }

  fun getTiles(bounds: LatLngBounds, zoomRange: IntRange): Flow<Pair<TileCoordinates, Tile>> =
    flow {
      val (worldZoomLevels, sliceZoomLevels) = zoomRange.partition { it < cellCogMinZoom }
      if (worldZoomLevels.isNotEmpty()) {
        emitAll(getTiles(TileCoordinates.WORLD, bounds, worldZoomLevels))
      }
      if (sliceZoomLevels.isNotEmpty()) {
        // Compute extents of first and last COG slice covered by specified bounds.
        val nwSlice = TileCoordinates.fromLatLng(bounds.northwest(), cellCogMinZoom)
        val seSlice = TileCoordinates.fromLatLng(bounds.southeast(), cellCogMinZoom)
        for (y in nwSlice.y..seSlice.y) {
          for (x in nwSlice.x..seSlice.x) {
            emitAll(getTiles(TileCoordinates(x, y, cellCogMinZoom), bounds, sliceZoomLevels))
          }
        }
      }
    }
  private fun getTiles(
    cogCoordinates: TileCoordinates,
    bounds: LatLngBounds,
    zoomLevels: List<Int>
  ): Flow<Pair<TileCoordinates, Tile>> = flow {
    val cog = getCog(cogCoordinates) ?: return@flow
    for (zoom in zoomLevels) {
      emitAll(cog.getTiles(TileCoordinates.withinBounds(bounds, zoom)))
    }
  }

  private fun readHeader(
    url: String,
    extent: TileCoordinates,
    inputStream: InputStream
  ): Mog {
    val startTimeMillis = System.currentTimeMillis()
    try {
      // This reads only headers and not the whole file.
      val tiff = TiffReader.readTiff(inputStream)
      val images = mutableListOf<MogImage>()
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
          MogImage(
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
      Timber.d("Loaded COG headers in $time ms")
      return Mog(url, images.toList())
    } catch (e: TiffException) {
      error("Failed to read COG: ${e.message})")
    } finally {
      inputStream.close()
    }
  }
}
