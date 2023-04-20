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

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.*

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

private fun String.toNulTerminatedByteArray() = this.toByteArray() + 0x00.toByte()

class CogImage(
  val cogFile: File,
  val originTile: TileCoordinates,
  val offsets: List<Long>,
  val byteCounts: List<Long>,
  val tileWidth: Short,
  val tileLength: Short,
  val imageWidth: Short,
  val imageLength: Short,
  // TODO: Use ByteArray instead?
  jpegTables: List<Byte>
) {
  val tileCountX = imageWidth / tileWidth
  val tileCountY = imageLength / tileLength
  private val jpegTablesBody =
    jpegTables
      .drop(2) // Skip extraneous SOI.
      .dropLast(2) // Skip extraneous EOI.
      .toByteArray()
  // TODO: Verify X and Y scales the same.
  //  val tiePointLatLng = CoordinateTransformer.webMercatorToWgs84(tiePointX, tiePointY)
  val zoomLevel = originTile.z // zoomLevelFromScale(pixelScaleY, tiePointLatLng.latitude)

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
    if (tile.z != zoomLevel)
      throw IllegalArgumentException("Requested z ($tile.z) != image z ($zoomLevel)")
    val xIdx = tile.x - originTile.x
    val yIdx = tile.y - originTile.y
    if (xIdx < 0 || yIdx < 0 || xIdx >= tileCountX || yIdx >= tileCountY) return null
    val idx = yIdx * tileCountX + xIdx
    if (idx > offsets.size) return null
    // TODO: Refactor, support HTTP.
    val raf = RandomAccessFile(cogFile, "r")
    val offset = offsets[idx] + 2 // Skip extraneous SOI
    val len = byteCounts[idx] - 2
    val imageBytes = ByteArray(len.toInt())
    raf.seek(offset)
    raf.read(imageBytes)
    return CogTile(tileWidth, tileLength, buildJpegTile(imageBytes))
  }

  override fun toString(): String {
    return "CogImage(cogFile=$cogFile, originTile=$originTile, offsets=.., byteCounts=.., tileWidth=$tileWidth, tileLength=$tileLength, imageWidth=$imageWidth, imageLength=$imageLength, tileCountX=$tileCountX, tileCountY=$tileCountY, jpegTablesBody=.., zoomLevel=$zoomLevel)"
  }
}
