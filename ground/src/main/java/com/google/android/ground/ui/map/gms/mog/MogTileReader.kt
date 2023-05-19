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

import com.google.android.gms.maps.model.Tile
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

class MogTileReader(private val inputStream: InputStream) {
  private var pos: Long = Long.MAX_VALUE

  fun readTile(tileMetadata: MogTileMetadata): Tile {
    val (_, imageMetadata, tileCoordinates, byteRange) = tileMetadata

    // Skip bytes for non-contiguous tile byte ranges.
    skipToPos(byteRange.first)

    val startTimeMillis = System.currentTimeMillis()

    val rawTileBytes = readTileBytes(inputStream, byteRange.count())

    val time = System.currentTimeMillis() - startTimeMillis
    Timber.d("Fetched tile ${tileCoordinates}: ${rawTileBytes.size} in $time ms")

    val jfifFileBytes = buildJfifFile(imageMetadata, rawTileBytes)
    return Tile(imageMetadata.tileWidth, imageMetadata.tileLength, jfifFileBytes)
  }

  private fun skipToPos(newPos: Long) {
    if (newPos < pos) error("Can't scan backwards in input stream")
    while (newPos > pos) {
      if (inputStream.read() == -1) error("Unexpected end of tile response")
      pos++
    }
  }

  /** Reads and returns a tile. Doesn't close the stream. */
  private fun readTileBytes(inputStream: InputStream, numBytes: Int): ByteArray {
    val bytes = ByteArray(numBytes)
    var bytesRead = 0
    while (bytesRead < numBytes) {
      val b = inputStream.read()
      if (b < 0) break
      bytes[bytesRead++] = b.toByte()
      pos++
    }
    if (bytesRead < numBytes) {
      Timber.w("Too few bytes received. Expected $numBytes, got $bytesRead")
    }
    return bytes
  }

  private fun buildJfifFile(imageMetadata: MogImageMetadata, imageBytes: ByteArray): ByteArray =
    START_OF_IMAGE +
      app0Segment(imageMetadata.tileWidth, imageMetadata.tileLength) +
      rawJpegTables(imageMetadata.jpegTables) +
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
