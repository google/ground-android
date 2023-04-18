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

import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.io.RandomAccessFile
import java.lang.Math.toRadians
import kotlin.math.*
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.FileDirectory
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

private fun String.toNulTerminatedByteArray() = this.toByteArray() + 0x00.toByte()

// TODO: Why is this falling just short of zoom level (thus using roundToInt())?
private fun zoomLevelFromScale(scale: Double, latitude: Double): Int =
  log2(C_EARTH * cos(toRadians(latitude)) / scale).roundToInt() - 8

/** Based on https://developers.google.com/maps/documentation/javascript/coordinates */
private fun LatLng.toWorldCoordinates(): WorldCoordinates {
  // Truncating to 0.9999 effectively limits latitude to 89.189. This is
  // about a third of a tile past the edge of the world tile.
//  if (latitude == 6.315298538330047 && longitude == 94.921875) {
//    Timber.e("HERE")
//  }
  var sinY = sin(latitude * PI / 180.0)
  sinY = sinY.coerceIn(-0.9999, 0.9999)
  return WorldCoordinates(
    TILE_SIZE * (0.5 + longitude / 360.0),
    TILE_SIZE * (0.5 - ln(((1 + sinY) / (1 - sinY))) / (4 * PI))
  )
}
/// ** Adapted from https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Kotlin */
// private fun LatLng.toTileCoordinates(zoom: Int): TileCoordinates {
//  // Number of tiles along a single dimension at the specified zoom level.
//  val zoomFactor = 1 shl zoom
//  val x = ((longitude + 180) / 360 * zoomFactor).toInt().coerceIn(0, zoomFactor - 1)
//  val y =
//    ((1.0 - asinh(tan(toRadians(latitude))) / PI) / 2 * zoomFactor)
//      .toInt()
//      .coerceIn(0, zoomFactor - 1)
//  // Requires y+1 for COG 9/391/248.. why?
//  return TileCoordinates(x, y, zoom)
// }

private fun LatLng.toTileCoordinates(z: Int): TileCoordinates =
  toWorldCoordinates().toTileCoordinates(z)

private fun fromMercatorToLatLng(x: Double, y: Double): LatLng {
  val n = PI - 2 * PI * y / 256
  val lng = x / 256 * 360 - 180
  val lat = 180 / PI * atan(0.5 * (exp(n) - exp(-n)))
  return LatLng(lat, lng)
}

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
  // TODO: Move tiepoints to Cog since they're the same for all images?
  val tileCountX = imageWidth / tileWidth
  val tileCountY = imageLength / tileLength
  // TODO: Verify X and Y scales the same.
  val geoAsciiParams = ifd.getStringEntryValue(FieldTagType.GeoAsciiParams)
  val tiePointLatLng = CoordinateTransformer.webMercatorToWgs84(tiePointX, tiePointY)
  val zoomLevel = zoomLevelFromScale(pixelScaleY, tiePointLatLng.latitude)
  val originTile = tiePointLatLng.toTileCoordinates(zoomLevel)
  val jpegTables =
    ifd
      .getLongListEntryValue(FieldTagType.JPEGTables)
      .map(Long::toByte)
      .drop(2) // Skip extraneous SOI.
      .dropLast(2) // Skip extraneous EOI.
      .toByteArray()
  // TODO: Verify geoAsciiParams is web mercator.
  // TODO: Verify tile size is 256x256.

  init {
    // https://developers.google.com/maps/documentation/javascript/examples/map-coordinates
    val world = tiePointLatLng.toWorldCoordinates()
    val tile = world.toTileCoordinates(zoomLevel)
    Timber.d(
      "--- File: ${cogFile.path} Origin: $tiePointLatLng M: ($tiePointX, $tiePointY) Tile: $originTile Z: $zoomLevel World: $world  Tile: $tile Origin Tile: $originTile"
    )
    //    Timber.d(fromMercatorToLatLng(tiePointX, tiePointY).toString())
  }

  private fun buildJpegTile(imageBytes: ByteArray): ByteArray =
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

  private val blankImage =
    File("/data/user/0/com.google.android.ground/files/blank.jpg").readBytes()

  fun getTile(x: Int, y: Int): CogTile? {
    val xIdx = x - originTile.x
    val yIdx = y - originTile.y
    // originTile.y is 246 instead of 247 for 9/391/247
    if (cogFile.path.endsWith("9/391/247.tif")) {
      Timber.e("---  x=$x y=$y xIdx=$xIdx yIdx=$yIdx z: $zoomLevel o: $originTile tie: $tiePointX, $tiePointY")
      return CogTile(256, 256, blankImage)
    }
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
