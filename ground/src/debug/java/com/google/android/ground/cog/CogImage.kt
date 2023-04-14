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

package com.google.android.ground.cog

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.FileDirectory

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

class CogImage(
  private val cogFile: File,
  ifd: FileDirectory,
  val pixelScaleX: Double = ifd.getDoubleListEntryValue(FieldTagType.ModelPixelScale)[0],
  val pixelScaleY: Double = ifd.getDoubleListEntryValue(FieldTagType.ModelPixelScale)[1],
  val tiePointX: Double = ifd.getDoubleListEntryValue(FieldTagType.ModelTiepoint)[3],
  val tiePointY: Double = ifd.getDoubleListEntryValue(FieldTagType.ModelTiepoint)[4]
) {
  val offsets: List<Long> = ifd.getLongListEntryValue(FieldTagType.TileOffsets)
  val byteCounts: List<Long> = ifd.getLongListEntryValue(FieldTagType.TileByteCounts)
  val tileWidth = ifd.getIntegerEntryValue(FieldTagType.TileWidth).toShort()
  val tileLength = ifd.getIntegerEntryValue(FieldTagType.TileLength).toShort()
  val imageWidth = ifd.getIntegerEntryValue(FieldTagType.ImageWidth).toShort()
  val imageLength = ifd.getIntegerEntryValue(FieldTagType.ImageLength).toShort()
  val tileCountX = imageWidth / tileWidth
  val tileCountY = imageLength / tileLength
  // TODO: Verify X and Y scales the same.
  val geoAsciiParams = ifd.getStringEntryValue(FieldTagType.GeoAsciiParams)
  val tiePointLatLng = CoordinateTransformer.webMercatorToWgs84(tiePointX, tiePointY)
  val zoomLevel =
    (log2(C_EARTH * cos(Math.toRadians(tiePointLatLng.latitude)) / pixelScaleY) - 8.0).roundToInt()
  val tiePointTileX = (tiePointLatLng.longitude * 2.0.pow(zoomLevel.toDouble()) / 256).toInt()
  val tiePointTileY = (tiePointLatLng.latitude * 2.0.pow(zoomLevel.toDouble()) / 256).toInt()
  val jpegTables =
    ifd
      .getLongListEntryValue(FieldTagType.JPEGTables)
      .map(Long::toByte)
      .drop(2) // Skip extraneous SOI.
      .dropLast(2) // Skip extraneous EOI.
      .toByteArray()
  // TODO: Verify geoAsciiParams is web mercator.
  // TODO: Verify that tile size is 256x256.

  fun tileIndex(x: Int, y: Int) = (x - tiePointTileX) + (y - tiePointTileY) * tileCountX

  fun buildJpegTile(imageBytes: ByteArray): ByteArray =
    START_OF_IMAGE + app0Segment(tileWidth, tileLength) + jpegTables + imageBytes + END_OF_IMAGE

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

  fun getTile(x: Int, y: Int): CogTile? {
    val xIdx = x - tiePointTileX
    val yIdx = y - tiePointTileY
    if (xIdx < 0 || yIdx < 0 || xIdx >= tileCountX || yIdx >= tileCountY) return null
    val idx = yIdx * tileCountX + xIdx
    if (idx > offsets.size) return null
    val raf = RandomAccessFile(cogFile, "r")
    val offset = offsets[idx] + 2 // Skip extraneous SOI
    val len = byteCounts[idx] - 2
    val imageBytes = ByteArray(len.toInt())
    raf.seek(offset)
    raf.read(imageBytes)
    return CogTile(tileWidth, tileLength, buildJpegTile(imageBytes))
  }
}

private fun byteArrayOf(vararg elements: Int) = elements.map(Int::toByte).toByteArray()

private fun Short.toByteArray() = byteArrayOf(this.toInt().shr(8).toByte(), this.toByte())

private fun String.toNulTerminatedByteArray() = this.toByteArray() + 0x00.toByte()
