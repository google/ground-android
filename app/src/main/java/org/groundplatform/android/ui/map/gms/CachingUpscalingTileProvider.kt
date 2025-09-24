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

import android.graphics.*
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
    if (z <= dataMaxZoom) return source.getTile(x, y, z) ?: TileProvider.NO_TILE

    val key = "$z/$x/$y"
    cache.get(key)?.let {
      return Tile(tileSize, tileSize, it)
    }

    val dz = z - dataMaxZoom
    val scale = 1 shl dz
    val srcX = x / scale
    val srcY = y / scale
    val qx = x % scale
    val qy = y % scale

    val src = source.getTile(srcX, srcY, dataMaxZoom) ?: return TileProvider.NO_TILE
    val bytes = src.data ?: return TileProvider.NO_TILE

    val srcBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return TileProvider.NO_TILE

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
      srcBmp.recycle()
      return TileProvider.NO_TILE
    }

    val cropped = Bitmap.createBitmap(srcBmp, cropLeft, cropTop, cropW, cropH)
    srcBmp.recycle()

    val upscaled = createBitmap(tileSize, tileSize)
    val canvas = Canvas(upscaled)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true } // bilinear
    canvas.drawBitmap(cropped, null, Rect(0, 0, tileSize, tileSize), paint)
    cropped.recycle()

    val out =
      ByteArrayOutputStream().use { os ->
        upscaled.compress(Bitmap.CompressFormat.PNG, 100, os)
        upscaled.recycle()
        os.toByteArray()
      }
    cache.put(key, out)
    return Tile(tileSize, tileSize, out)
  }
}
