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
import java.lang.System.currentTimeMillis
import java.net.HttpURLConnection
import java.net.URL
import timber.log.Timber

/* Circumference of the Earth (m) */
private const val C_EARTH = 40075016.686
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

private fun IntArray.toByteArray(): ByteArray = this.copyOf().map { it.toByte() }.toByteArray()

private fun String.toNulTerminatedByteArray() = this.toByteArray() + 0x00.toByte()

private fun Bitmap.isMostlyBlank(): Boolean {
  for (x in 0 until width) {
    for (y in 0 until height) {
      val color = getPixel(x, y)
      val r: Int = color shr 16 and 0xFF
      val g: Int = color shr 8 and 0xFF
      val b: Int = color shr 0 and 0xFF
      if (r + g + b > 10) {
        return false
      }
    }
  }
  return true
}

private fun Bitmap.setAllPixels(color: Int) {
  for (x in 0 until width) {
    for (y in 0 until height) {
      setPixel(x, y, color)
    }
  }
}

const val NIL = 0x00.toByte()

class CogImage(
  val url: URL,
  val originTile: TileCoordinates,
  val offsets: List<Long>,
  val byteCounts: List<Long>,
  val tileWidth: Short,
  val tileLength: Short,
  val imageWidth: Short,
  val imageLength: Short,
  // TODO: Use ByteArray instead?
  jpegTables: List<Byte>?
) {
  val tileCountX = imageWidth / tileWidth
  val tileCountY = imageLength / tileLength
  private val jpegTablesBody =
    (jpegTables ?: listOf())
      .drop(2) // Skip extraneous SOI.
      .dropLast(2) // Skip extraneous EOI.
      .toByteArray()
  // TODO: Verify X and Y scales the same.
  //  val tiePointLatLng = CoordinateTransformer.webMercatorToWgs84(tiePointX, tiePointY)
  val zoomLevel = originTile.zoom // zoomLevelFromScale(pixelScaleY, tiePointLatLng.latitude)

  // TODO: Rename to something more self descriptive.
  private fun buildJpegTile(imageBytes: ByteArray): ByteArray =
    START_OF_IMAGE + app0Segment(tileWidth, tileLength) + jpegTablesBody + imageBytes + END_OF_IMAGE

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

  //  private val blankImage =
  //    File("/data/user/0/com.google.android.ground/files/blank.jpg").readBytes()

  fun getTile(tile: TileCoordinates): CogTile? {
    if (tile.zoom != zoomLevel)
      throw IllegalArgumentException("Requested z ($tile.z) != image z ($zoomLevel)")
    val xIdx = tile.x - originTile.x
    val yIdx = tile.y - originTile.y
    if (xIdx < 0 || yIdx < 0 || xIdx >= tileCountX || yIdx >= tileCountY) return null
    val idx = yIdx * tileCountX + xIdx
    if (idx > offsets.size) return null
    val from = offsets[idx] + 2 // Skip extraneous SOI
    val len = byteCounts[idx].toInt() - 2
    val to = from + len - 1

    val startTimeMillis = currentTimeMillis()
    val urlConnection = url.openConnection() as HttpURLConnection
    urlConnection.requestMethod = "GET"
    urlConnection.setRequestProperty("Range", "bytes=$from-$to")
    urlConnection.readTimeout = 5 * 1000
    urlConnection.connect()
    val inputStream = urlConnection.inputStream
    try {
      val responseCode = urlConnection.responseCode
      if (responseCode == 404 || responseCode != 206) {
        Timber.d(
          "Failed to load tile ${tile.x},${tile.y} @ zoom ${tile.zoom}. HTTP $responseCode on $url"
        )
        return null
      }
      val imageBytes = ByteArray(len)
      var bytesRead = 0
      var i: Int
      // TODO: Pipe buildJpegTile() into stream and let BitmapFactory read input stream instead.
      while (inputStream.read().also { i = it } != -1) {
        imageBytes[bytesRead++] = i.toByte()
      }
      val time = currentTimeMillis() - startTimeMillis
      Timber.d(
        "Fetched tile ${tile.x},${tile.y} @ zoom ${tile.zoom}: ${bytesRead} bytes in $time ms from $url"
      )
      if (bytesRead < len) {
        Timber.d("Read incomplete: $bytesRead of $len bytes read.")
      }

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

      return CogTile(tileWidth, tileLength, out.toByteArray())

      //        if (imageBytes.all { it == NIL }) return null
      //      return CogTile(tileWidth, tileLength, buildJpegTile(imageBytes))
    } finally {
      inputStream.close()
      urlConnection.disconnect()
    }
    // TODO: Add support for local files using:
    //    val raf = RandomAccessFile(cogFile, "r")
    //    val imageBytes = ByteArray(len.toInt())
    //    raf.seek(offset)
    //    raf.read(imageBytes)
  }

  override fun toString(): String {
    return "CogImage(url=$url, originTile=$originTile, offsets=.., byteCounts=.., tileWidth=$tileWidth, tileLength=$tileLength, imageWidth=$imageWidth, imageLength=$imageLength, tileCountX=$tileCountX, tileCountY=$tileCountY, jpegTablesBody=.., zoomLevel=$zoomLevel)"
  }
}
