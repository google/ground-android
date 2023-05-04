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

package com.google.android.ground.ui.map.gms.cog

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.System.currentTimeMillis
import timber.log.Timber

/* Circumference of the Earth (m) */
private val START_OF_IMAGE = byteArrayOf(0xFF, 0xD8)
private val APP0_MARKER = byteArrayOf(0xFF, 0xE0)
// Marker segment length with no thumbnails.
private const val APP0_MIN_LEN: Short = 16
private const val JFIF_IDENTIFIER = "JFIF"
private const val JFIF_MAJOR_VERSION = 1
private const val JFIF_MINOR_VERSION = 2
private const val NO_DENSITY_UNITS = 0
private val END_OF_IMAGE = byteArrayOf(0xFF, 0xD9)

private fun byteArrayOf(vararg elements: Int) = elements.map(Int::toByte).toByteArray()

private fun Short.toByteArray() = byteArrayOf(this.toInt().shr(8).toByte(), this.toByte())

private fun String.toNulTerminatedByteArray() = this.toByteArray() + 0x00.toByte()

class CogTileParser(val image: CogImage) {
  /** Input stream is not closed. */
  fun parseTile(coordinates: TileCoordinates, inputStream: InputStream): CogTile {
    if (!image.hasTile(coordinates))
      throw IllegalArgumentException("Requested $coordinates out of image bounds")
    val xIdx = coordinates.x - image.originTile.x
    val yIdx = coordinates.y - image.originTile.y
    val idx = yIdx * image.tileCountX + xIdx
    if (idx > image.offsets.size) throw IllegalArgumentException("idx > offsets")
    // TODO: Use image.getByteRange().count here instead
    val len = image.byteCounts[idx].toInt() - 2
    val startTimeMillis = currentTimeMillis()
    val imageBytes = ByteArray(len)
    var bytesRead = 0
    // TODO: Pipe buildJpegTile() into stream and let BitmapFactory read input stream instead.
    while (bytesRead < len) {
      val b = inputStream.read()
      if (b < 0) break
      imageBytes[bytesRead++] = b.toByte()
    }
    val time = currentTimeMillis() - startTimeMillis
    Timber.d("Fetched tile ${coordinates}: $bytesRead of $len bytes in $time ms")

    // Crude method of making missing pixels transparent. Ideally, rather than replacing dark
    // pixels with transparent once, we would use the image masks contained in the COG. This
    // method was used for expediency.
    val bitmap =
      BitmapFactory.decodeByteArray(buildJpegTile(imageBytes), 0, len)
        .copy(Bitmap.Config.ARGB_8888, true)
    bitmap.setHasAlpha(true)
    for (x in 0 until bitmap.width) {
      for (y in 0 until bitmap.height) {
        val color = bitmap.getPixel(x, y)
        val r: Int = color shr 16 and 0xFF
        val g: Int = color shr 8 and 0xFF
        val b: Int = color shr 0 and 0xFF
        if (r + g + b < 50) {
          bitmap.setPixel(x, y, 0)
        }
      }
    }
    val out = ByteArrayOutputStream()
    // TODO: Manually build and return BMP instead of recompressing.
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)

    return CogTile(coordinates, image.tileWidth, image.tileLength, out.toByteArray())

    //        if (imageBytes.all { it == NIL }) return null
    //      return CogTile(tileWidth, tileLength, buildJpegTile(imageBytes))

    // TODO: Add support for local files using:
    //    val raf = RandomAccessFile(cogFile, "r")
    //    val imageBytes = ByteArray(len.toInt())
    //    raf.seek(offset)
    //    raf.read(imageBytes)
  }

  /*
    fun getTiles(bounds: LatLngBounds): Flow<Result<CogTile>> = flow {
      val nwTile = TileCoordinates.fromLatLng(bounds.northwest(), zoomLevel)
      val seTile = TileCoordinates.fromLatLng(bounds.southeast(), zoomLevel)
      for (y in nwTile.y..seTile.y) {
        for (x in nwTile.x..seTile.x) {
          try {
            val coordinates = TileCoordinates(x, y, zoomLevel)
            if (hasTile(coordinates)) emit(success(getTile(coordinates)))
          } catch (e: Throwable) {
            emit(failure(e))
          }
        }
      }
    }
  */
  private fun buildJpegTile(imageBytes: ByteArray): ByteArray =
    START_OF_IMAGE +
      app0Segment(image.tileWidth, image.tileLength) +
      ((image.jpegTables ?: listOf())
        .drop(2) // Skip extraneous SOI.
        .dropLast(2) // Skip extraneous EOI.
        .toByteArray()) +
      imageBytes +
      END_OF_IMAGE

  /** Build "Application Segment 0" section of header. */
  private fun app0Segment(tileWidth: Short, tileHeight: Short) =
    APP0_MARKER +
      APP0_MIN_LEN.toByteArray() +
      JFIF_IDENTIFIER.toNulTerminatedByteArray() +
      JFIF_MAJOR_VERSION.toByte() +
      JFIF_MINOR_VERSION.toByte() +
      NO_DENSITY_UNITS.toByte() +
      tileWidth.toByteArray() +
      tileHeight.toByteArray() +
      byteArrayOf(0, 0) // Dimensions of empty thumbnail.
}
