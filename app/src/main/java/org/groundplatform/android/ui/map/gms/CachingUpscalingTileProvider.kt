/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.map.gms

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache
import androidx.core.graphics.createBitmap
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import java.io.ByteArrayOutputStream
import timber.log.Timber

/**
 * A [TileProvider] implementation that caches and upscales map tiles when higher zoom levels are
 * requested than those available in the base data source.
 *
 * This class is typically used when offline or low-resolution tiles must be reused at higher zoom
 * levels by cropping and bilinearly upscaling them to fit the target resolution.
 *
 * **Performance Note:** Upscaling operations involve decoding and re-encoding bitmaps in memory. To
 * prevent UI jank or ANRs, callers should invoke this provider within appropriate coroutines or
 * background dispatchers (e.g., `Dispatchers.IO`).
 *
 * @param source The base [TileProvider] supplying the original imagery.
 * @param dataMaxZoom The maximum zoom level available in the source tiles.
 * @param tileSize The pixel dimension of each tile (default = 256).
 * @param maxCacheBytes The maximum cache size (in bytes) for storing synthesized tiles (default =
 *   16 MB).
 */
class CachingUpscalingTileProvider(
  private val source: TileProvider,
  private val dataMaxZoom: Int,
  private val tileSize: Int = DEFAULT_TILE_SIZE,
  maxCacheBytes: Int = DEFAULT_CACHE_SIZE_BYTES,
) : TileProvider {

  /** In-memory cache for synthesized tiles, keyed by "zoom/x/y". */
  private val cache =
    object : LruCache<String, ByteArray>(maxCacheBytes) {
      override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

  /**
   * Retrieves a map tile for the given coordinates.
   * - If [z] ≤ [dataMaxZoom], the tile is fetched directly from [source].
   * - If [z] > [dataMaxZoom], an upscaled tile is synthesized from lower-zoom data and cached.
   */
  override fun getTile(x: Int, y: Int, z: Int): Tile {
    // Base-level tiles: delegate directly to source
    if (z <= dataMaxZoom) return source.getTile(x, y, z) ?: TileProvider.NO_TILE

    val key = "$z/$x/$y"
    cache.get(key)?.let { cachedBytes ->
      return Tile(tileSize, tileSize, cachedBytes)
    }

    val synthesizedBytes = synthesizeUpscaledTileBytes(x, y, z)
    return synthesizedBytes?.let { bytes ->
      cache.put(key, bytes)
      Tile(tileSize, tileSize, bytes)
    } ?: TileProvider.NO_TILE
  }

  /** Builds a PNG tile byte array by cropping and upscaling a lower-zoom tile from [source]. */
  private fun synthesizeUpscaledTileBytes(x: Int, y: Int, z: Int): ByteArray? {
    val zoomDelta = z - dataMaxZoom
    val scale = 1 shl zoomDelta
    val srcX = x / scale
    val srcY = y / scale
    val quadrantX = x % scale
    val quadrantY = y % scale

    var decoded: Bitmap? = null
    return try {
      val sourceBytes = source.getTile(srcX, srcY, dataMaxZoom)?.data
      decoded = sourceBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

      val cropRect =
        decoded?.let { bmp ->
          val cropWidth = bmp.width / scale
          val cropHeight = bmp.height / scale
          val left = quadrantX * cropWidth
          val top = quadrantY * cropHeight
          if (
            left >= 0 && top >= 0 && left + cropWidth <= bmp.width && top + cropHeight <= bmp.height
          ) {
            Rect(left, top, left + cropWidth, top + cropHeight)
          } else null
        }

      decoded?.let { bitmap -> cropRect?.let { rect -> drawUpscaledTile(bitmap, rect) } }
    } catch (t: Throwable) {
      Timber.d(t, "Failed to synthesize upscaled tile for z=$z ($x,$y)")
      null
    } finally {
      decoded?.recycle()
    }
  }

  /**
   * Crops a bitmap to the given [cropRect] and upscales it to a [tileSize] × [tileSize] PNG using
   * bilinear filtering.
   */
  private fun drawUpscaledTile(src: Bitmap, cropRect: Rect): ByteArray? {
    var cropped: Bitmap? = null
    var upscaled: Bitmap? = null
    return try {
      cropped =
        Bitmap.createBitmap(src, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
      upscaled = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)

      val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
      Canvas(upscaled).drawBitmap(cropped, null, Rect(0, 0, tileSize, tileSize), paint)

      ByteArrayOutputStream().use { output ->
        upscaled.compress(Bitmap.CompressFormat.PNG, 100, output)
        output.toByteArray()
      }
    } catch (t: Throwable) {
      Timber.d(t, "Error during tile upscaling")
      null
    } finally {
      cropped?.recycle()
      upscaled?.recycle()
    }
  }

  companion object {
    /** Default tile size in pixels (Google Maps standard = 256). */
    private const val DEFAULT_TILE_SIZE = 256

    /** Default maximum cache size in bytes (~16 MB). */
    private const val DEFAULT_CACHE_SIZE_BYTES = 16 * 1024 * 1024
  }
}
