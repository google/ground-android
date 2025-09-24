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
          if (bytes != null) {
            cache.put(key, bytes)
            Tile(tileSize, tileSize, bytes)
          } else {
            null
          }
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

    val srcTile = source.getTile(srcX, srcY, dataMaxZoom) ?: return null
    val bytes = srcTile.data ?: return null

    var srcBmp: Bitmap? = null
    var cropped: Bitmap? = null
    var upscaled: Bitmap? = null
    return try {
      srcBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

      val cropW = srcBmp.width / scale
      val cropH = srcBmp.height / scale
      val cropLeft = qx * cropW
      val cropTop = qy * cropH
      if (
        cropLeft < 0 ||
          cropTop < 0 ||
          cropLeft + cropW > srcBmp.width ||
          cropTop + cropH > srcBmp.height
      ) {
        return null
      }

      cropped = Bitmap.createBitmap(srcBmp, cropLeft, cropTop, cropW, cropH)

      upscaled = createBitmap(tileSize, tileSize)
      val canvas = Canvas(upscaled)
      val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true } // bilinear
      canvas.drawBitmap(cropped, null, Rect(0, 0, tileSize, tileSize), paint)

      ByteArrayOutputStream().use { os ->
        upscaled.compress(Bitmap.CompressFormat.PNG, 100, os)
        os.toByteArray()
      }
    } catch (_: Throwable) {
      null
    } finally {
      // Ensure we always recycle to avoid memory pressure while panning/zooming
      upscaled?.recycle()
      cropped?.recycle()
      srcBmp?.recycle()
    }
  }
}
