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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Tile
import java.io.ByteArrayOutputStream
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

/** A collection of Maps Optimized GeoTIFFs (MOGs). */
class MogCollection(
  private val worldMogUrl: String,
  private val regionMogUrlTemplate: String,
  private val regionMogMinZoom: Int,
  val regionMogMaxZoom: Int
) {
  private val cache: LruCache<String, Deferred<Mog?>> = LruCache(16)

  private fun getMogExtentForTile(tileCoordinates: TileCoordinates): TileCoordinates =
    if (tileCoordinates.zoom < regionMogMinZoom) TileCoordinates.WORLD
    else tileCoordinates.originAtZoom(regionMogMinZoom)

  /** Returns the MOG containing the tile with the specified coordinates. */
  private suspend fun getMogForTile(tileCoordinates: TileCoordinates): Mog? =
    getMog(getMogExtentForTile(tileCoordinates))

  private suspend fun getMog(extent: TileCoordinates): Mog? =
    getOrFetchMogAsync(getMogUrl(extent), extent).await()

  private fun getMogUrl(extent: TileCoordinates): String {
    val (x, y, zoom) = extent
    if (zoom == 0) {
      return worldMogUrl
    }
    if (zoom < regionMogMinZoom) {
      error("Invalid zoom for this collection. Expected 0 or $regionMogMinZoom, got $zoom")
    }
    return regionMogUrlTemplate
      .replace("{x}", x.toString())
      .replace("{y}", y.toString())
      .replace("{z}", zoom.toString())
  }

  /**
   * Returns a future containing the [Mog] with the specified extent and URL. Metadata is either
   * loaded from in-memory cache if present, or asynchronously fetched if necessary. The process of
   * checking the cache and creating the new job is synchronized on the current instance to prevent
   * duplicate parallel requests for the same resource.
   */
  private fun getOrFetchMogAsync(url: String, extent: TileCoordinates): Deferred<Mog?> =
    synchronized(this) { cache.get(url) ?: fetchMogAsync(url, extent) }

  /**
   * Asynchronously fetches and returns the [Mog] with the specified extent and URL. The async job
   * is added to the cache immediately to prevent duplicate fetches from other threads.
   */
  private fun fetchMogAsync(url: String, extent: TileCoordinates): Deferred<Mog?> = runBlocking {
    // TODO: Exceptions get propagated as cancellation of the coroutine. Handle them!
    @Suppress("DeferredResultUnused")
    async { nullIfNotFound { UrlInputStream(url) }?.use { readHeader(url, extent, it) } }
      .also { cache.put(url, it) }
  }

  suspend fun getTile(tileCoordinates: TileCoordinates): Tile? =
    getMogForTile(tileCoordinates)?.getTile(tileCoordinates)

  fun getTiles(bounds: LatLngBounds, zoomRange: IntRange): Flow<Pair<TileCoordinates, Tile>> =
    flow {
      val (worldZoomLevels, sliceZoomLevels) = zoomRange.partition { it < regionMogMinZoom }
      if (worldZoomLevels.isNotEmpty()) {
        emitAll(getTiles(TileCoordinates.WORLD, bounds, worldZoomLevels))
      }
      if (sliceZoomLevels.isNotEmpty()) {
        // Compute extents of first and last region covered by specified bounds.
        val nwSlice = TileCoordinates.fromLatLng(bounds.northwest(), regionMogMinZoom)
        val seSlice = TileCoordinates.fromLatLng(bounds.southeast(), regionMogMinZoom)
        for (y in nwSlice.y..seSlice.y) {
          for (x in nwSlice.x..seSlice.x) {
            emitAll(getTiles(TileCoordinates(x, y, regionMogMinZoom), bounds, sliceZoomLevels))
          }
        }
      }
    }
  private fun getTiles(
    mogExtent: TileCoordinates,
    bounds: LatLngBounds,
    zoomLevels: List<Int>
  ): Flow<Pair<TileCoordinates, Tile>> = flow {
    val mog = getMog(mogExtent) ?: return@flow
    for (zoom in zoomLevels) {
      emitAll(mog.getTiles(TileCoordinates.withinBounds(bounds, zoom)))
    }
  }

  private fun readHeader(url: String, extent: TileCoordinates, inputStream: InputStream): Mog {
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
      Timber.d("Loaded headers from $url in $time ms")

      return Mog(url, images.toList())
    } catch (e: TiffException) {
      error("Failed to read $url: ${e.message})")
    } finally {
      inputStream.close()
    }
  }

  /**
   * Crude method of making missing pixels transparent. Ideally, rather than replacing dark pixels
   * with transparent ones, we would use the image masks contained.
   */
  fun applyMask(tile: Tile?, tileCoordinates: TileCoordinates): Tile? {
    // Only apply mask workaround to world COG for now.
    if (tile?.data == null || tileCoordinates.zoom >= regionMogMinZoom) {
      return tile
    }
    val bitmap =
      BitmapFactory.decodeByteArray(tile.data, 0, tile.data!!.size)
        .copy(Bitmap.Config.ARGB_8888, true)
    bitmap.setHasAlpha(true)
    for (x in 0 until bitmap.width) {
      for (y in 0 until bitmap.height) {
        val color = bitmap.getPixel(x, y)
        val r: Int = color shr 16 and 0xFF
        val g: Int = color shr 8 and 0xFF
        val b: Int = color shr 0 and 0xFF
        if (r + g + b == 0) {
          bitmap.setPixel(x, y, 0)
        }
      }
    }
    val out = ByteArrayOutputStream()
    // Note: JPEG doesn't support transparency, so need to use PNG or BMP.
    // TODO: Return raw BMP instead of recompressing to JPEG.
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    return Tile(tile.width, tile.height, out.toByteArray())
  }
}

private inline fun <T> nullIfNotFound(fn: () -> T) =
  try {
    fn()
  } catch (_: FileNotFoundException) {
    null
  }
