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

import java.io.InputStream
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

class MogImage(
  val tileWidth: Int,
  val tileLength: Int,
  private val originTile: TileCoordinates,
  private val offsets: List<Long>,
  private val byteCounts: List<Long>,
  private val imageWidth: Int,
  private val imageLength: Int,
  private val jpegTables: ByteArray
) {
  private val tileCountX = imageWidth / tileWidth
  private val tileCountY = imageLength / tileLength

  // TODO: Verify X and Y scales the same.
  val zoom = originTile.zoom

  private fun hasTile(x: Int, y: Int) =
    x >= originTile.x &&
      y >= originTile.y &&
      x < tileCountX + originTile.x &&
      y < tileCountY + originTile.y

  override fun toString(): String {
    return "MogImage(originTile=$originTile, offsets=.., byteCounts=.., tileWidth=$tileWidth, tileLength=$tileLength, imageWidth=$imageWidth, imageLength=$imageLength, tileCountX=$tileCountX, tileCountY=$tileCountY, jpegTables=.., zoom=$zoom)"
  }

  fun getByteRange(x: Int, y: Int): LongRange? {
    if (!hasTile(x, y)) return null
    val xIdx = x - originTile.x
    val yIdx = y - originTile.y
    val idx = yIdx * tileCountX + xIdx
    if (idx > offsets.size) throw IllegalArgumentException("idx > offsets")
    val from = offsets[idx]
    val len = byteCounts[idx].toInt()
    val to = from + len - 1
    return from..to
  }

  /** Input stream is not closed. */
  fun parseTile(inputStream: InputStream, numBytes: Int): ByteArray {
    val imageBytes = ByteArray(numBytes)
    var bytesRead = 0
    while (bytesRead < numBytes) {
      val b = inputStream.read()
      if (b < 0) break
      imageBytes[bytesRead++] = b.toByte()
    }
    if (bytesRead < numBytes) {
      Timber.w("Too few bytes received. Expected $numBytes, got $bytesRead")
    }

    return buildJfifFile(imageBytes)
  }

  private fun buildJfifFile(imageBytes: ByteArray): ByteArray =
    START_OF_IMAGE +
      app0Segment(tileWidth, tileLength) +
      rawJpegTables(jpegTables) +
      imageBytes.drop(2) + // Drop leading SOI.
      END_OF_IMAGE

  private fun rawJpegTables(jpegTables: ByteArray): ByteArray =
    jpegTables
      .drop(2) // Drop leading SOI.
      .dropLast(2) // Drop trailing EOI.
      .toByteArray()

  /** Build "Application Segment 0" section of header. */
  private fun app0Segment(tileWidth: Int, tileHeight: Int) =
    APP0_MARKER +
      APP0_MIN_LEN.toByteArray() +
      JFIF_IDENTIFIER.toNulTerminatedByteArray() +
      JFIF_MAJOR_VERSION.toByte() +
      JFIF_MINOR_VERSION.toByte() +
      NO_DENSITY_UNITS.toByte() +
      tileWidth.toShort().toByteArray() +
      tileHeight.toShort().toByteArray() +
      kotlin.byteArrayOf(0, 0) // Dimensions of empty thumbnail.
}
