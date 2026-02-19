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
 * A [TileProvider] that provides upscaled map tiles beyond the available data zoom level.
 *
 * This provider attempts to synthesize higher-zoom tiles by:
 * - Fetching the base tile at [zoomThreshold]
 * - Cropping the appropriate quadrant
 * - Upscaling it using bilinear filtering to a 256×256 PNG
 *
 * **Performance Note:** The upscaling operation performed by [synthesizeUpscaledTileBytes] is **CPU
 * and memory intensive** due to bitmap decoding and scaling. Callers should ensure this provider
 * runs in a background coroutine with an appropriate [CoroutineDispatcher] (e.g. `Dispatchers.IO`)
 * to prevent blocking the main thread or UI rendering.
 *
 * Typical usage:
 * ```kotlin
 * val provider = CachingUpscalingTileProvider(source, dataMaxZoom)
 * val overlay = map.addTileOverlay(
 *     TileOverlayOptions().tileProvider(provider)
 * )
 * ```
 *
 * @param source The underlying [TileProvider] that supplies base tiles up to [zoomThreshold].
 * @param zoomThreshold The maximum zoom level for which base tiles are available. Tiles beyond this
 *   level will be synthesized by cropping and upscaling.
 * @param tileSize The size (in pixels) of each output tile. Defaults to [DEFAULT_TILE_SIZE] (256
 *   px).
 * @param maxCacheBytes The maximum in-memory cache size in bytes for storing upscaled tiles.
 */
class CachingUpscalingTileProvider(
  private val source: TileProvider,
  private val zoomThreshold: Int,
) : TileProvider {

  private val cache =
    object : LruCache<String, ByteArray>(DEFAULT_CACHE_SIZE_BYTES) {
      override fun sizeOf(key: String, value: ByteArray) = value.size
    }

  override fun getTile(x: Int, y: Int, zoom: Int): Tile {
    val cacheKey = "$zoom/$x/$y"
    var result: Tile? = null

    if (zoom <= zoomThreshold) {
      result = source.getTile(x, y, zoom)
    }

    if (result == null && cache.get(cacheKey) != null) {
      result = Tile(DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE, cache.get(cacheKey))
    }

    if (result == null) {

      // TODO: Upscaling optimisation -
      // https://github.com/google/ground-android/pull/3259#discussion_r2454376062
      val upscaledBytes = synthesizeUpscaledTileBytes(x, y, zoom)
      if (upscaledBytes != null) {
        cache.put(cacheKey, upscaledBytes)
        result = Tile(DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE, upscaledBytes)
      }
    }

    return result ?: TileProvider.NO_TILE
  }

  /**
   * Build a 256×256 PNG byte array by cropping the appropriate quadrant of the source tile at
   * z=dataMaxZoom and upscaling it with bilinear filtering. Returns null on any failure.
   */
  private fun synthesizeUpscaledTileBytes(x: Int, y: Int, z: Int): ByteArray? {
    val dz = z - zoomThreshold
    val scale = 1 shl dz
    val srcX = x / scale
    val srcY = y / scale
    val qx = x % scale
    val qy = y % scale

    var decoded: Bitmap? = null
    var result: ByteArray? = null

    try {
      val bytes: ByteArray? = source.getTile(srcX, srcY, zoomThreshold)?.data
      decoded = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

      val crop: Rect? =
        decoded?.let { bmp ->
          val cw = bmp.width / scale
          val ch = bmp.height / scale
          val left = qx * cw
          val top = qy * ch
          if (left >= 0 && top >= 0 && left + cw <= bmp.width && top + ch <= bmp.height) {
            Rect(left, top, left + cw, top + ch)
          } else {
            null
          }
        }

      result = decoded?.let { d -> crop?.let { c -> drawUpscaled256(d, c) } }
    } catch (_: Throwable) {} finally {
      decoded?.recycle()
    }

    return result
  }

  /** Crops the given area and upscales to 256×256 PNG using bilinear filtering. */
  private fun drawUpscaled256(src: Bitmap, crop: Rect): ByteArray? {
    var up: Bitmap? = null
    return try {
      up = createBitmap(DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE)
      val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

      // Draw directly from the source bitmap region into the 256×256 tile
      Canvas(up)
        .drawBitmap(
          src,
          crop, // use the source rect; no intermediate crop allocation
          Rect(0, 0, 256, 256),
          paint,
        )

      // Use WebP lossless to keep bytes smaller (optional; PNG also fine)
      ByteArrayOutputStream(32_768).use { os ->
        up.compress(Bitmap.CompressFormat.WEBP, 100, os)
        os.toByteArray()
      }
    } catch (oom: OutOfMemoryError) {
      Timber.w(oom, "OOM while upscaling tile (crop=$crop, src=${src.width}x${src.height})")
      null
    } catch (t: Throwable) {
      Timber.d(t, "Failed to draw upscaled tile (crop=$crop, src=${src.width}x${src.height})")
      null
    } finally {
      up?.recycle()
    }
  }

  companion object {
    /** Default tile size in pixels (Google Maps standard = 256). */
    private const val DEFAULT_TILE_SIZE = 256

    /**
     * Default maximum cache size in bytes (~16 MB).
     *
     * 💡 Approximation:
     * - Each 256×256 tile (JPEG/WebP) ≈ 20–40 KB.
     * - 16 MB cache can hold roughly 400 – 800 tiles.
     * - This is generally sufficient for a few zoom levels worth of visible area.
     *
     * If using uncompressed PNG tiles (~196 KB each), the cache fits only ~80 tiles.
     */
    private const val DEFAULT_CACHE_SIZE_BYTES = 16 * 1024 * 1024
  }
}
