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

/** Wraps a TileProvider and synthesizes tiles for z > dataMaxZoom via crop+upscale. */
class CachingUpscalingTileProvider(
  private val source: TileProvider,
  private val dataMaxZoom: Int,
  private val tileSize: Int = 256,
  maxCacheBytes: Int = 16 * 1024 * 1024, // ~16 MB
) : TileProvider {

  private val cache =
    object : LruCache<String, ByteArray>(maxCacheBytes) {
      override fun sizeOf(key: String, value: ByteArray) = value.size
    }

  override fun getTile(x: Int, y: Int, z: Int): Tile {
    var result: Tile?

    if (z <= dataMaxZoom) {
      // Base tiles: just delegate; no caching here
      result = source.getTile(x, y, z)
    } else {
      val key = "$z/$x/$y"
      val cached = cache.get(key)
      result =
        if (cached != null) {
          Tile(tileSize, tileSize, cached)
        } else {
          val bytes = synthesizeUpscaledTileBytes(x, y, z)
          bytes?.let {
            cache.put(key, bytes)
            Tile(tileSize, tileSize, bytes)
          } ?: run { null }
        }
    }

    return result ?: TileProvider.NO_TILE
  }

  /**
   * Build a 256×256 PNG byte array by cropping the appropriate quadrant of the source tile at
   * z=dataMaxZoom and upscaling it with bilinear filtering. Returns null on any failure.
   */
  private fun synthesizeUpscaledTileBytes(x: Int, y: Int, z: Int): ByteArray? {
    val dz = z - dataMaxZoom
    val scale = 1 shl dz
    val srcX = x / scale
    val srcY = y / scale
    val qx = x % scale
    val qy = y % scale

    var decoded: Bitmap? = null
    var result: ByteArray? = null

    try {
      val bytes: ByteArray? = source.getTile(srcX, srcY, dataMaxZoom)?.data
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
    var cropped: Bitmap? = null
    var up: Bitmap? = null
    return try {
      cropped = Bitmap.createBitmap(src, crop.left, crop.top, crop.width(), crop.height())
      up = createBitmap(256, 256)

      val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
      Canvas(up).drawBitmap(cropped, null, Rect(0, 0, 256, 256), paint)

      ByteArrayOutputStream().use { os ->
        up.compress(Bitmap.CompressFormat.PNG, 100, os)
        os.toByteArray()
      }
    } catch (_: Throwable) {
      null
    } finally {
      up?.recycle()
      cropped?.recycle()
    }
  }
}
