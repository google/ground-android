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
package com.google.android.ground

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.log2
import mil.nga.tiff.FieldTagType.*
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffReader
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import java.lang.Math.*
import kotlin.math.pow

val START_OF_IMAGE = byteArrayOf(0xFF, 0xD8)
val APP0_MARKER = byteArrayOf(0xFF, 0xE0)
// Marker segment length with no thumbnails.
const val APP0_MIN_LEN: Short = 16
const val JFIF_IDENTIFIER = "JFIF"
const val JFIF_MAJOR_VERSION = 1
const val JFIF_MINOR_VERSION = 2
const val NO_DENSITY_UNITS = 0
val END_OF_IMAGE = byteArrayOf(0xFF, 0xD9)

// Instrumentation needed to use Android SDK's BitmapFactory.
@Suppress("UNCHECKED_CAST")
@HiltAndroidTest
class CogTest {
  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @Inject @ApplicationContext lateinit var context: Context

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun extractTiles() {
    val cogX = 391
    val cogY = 251
    val cogZ = 9
    val x = 31
    val y = 31
    val z = 14
    val cogMaxZoom = 14

    val cogFile = File(context.filesDir, "cogs/9/391/251.tif")
    val tiff: TIFFImage = TiffReader.readTiff(cogFile)
    // IFDs are in decreasing detail (decreasing zoom), starting with max, ending with min zoom.
    //    val cogMaxZoom = cogMinZoom + tiff.fileDirectories.size - 1
    val image = tiff.fileDirectories[0]
    val offsets = image.getLongListEntryValue(TileOffsets)
    val byteCounts = image.getLongListEntryValue(TileByteCounts)
    val tileWidth = image.getIntegerEntryValue(TileWidth).toShort()
    val tileLength = image.getIntegerEntryValue(TileLength).toShort()
    val imageWidth = image.getIntegerEntryValue(ImageWidth).toShort()
    val imageLength = image.getIntegerEntryValue(ImageLength).toShort()
    val pixelScaleX = image.getDoubleListEntryValue(ModelPixelScale)[0]
    val pixelScaleY = image.getDoubleListEntryValue(ModelPixelScale)[1]
    // TODO: Verify X and Y scales the same.
    val tiePointX = image.getDoubleListEntryValue(ModelTiepoint)[3]
    val tiePointY = image.getDoubleListEntryValue(ModelTiepoint)[4]
    val geoAsciiParams = image.getStringEntryValue(GeoAsciiParams)
    // TODO: Verify geoAsciiParams is web mercator.
    val crsFactory = CRSFactory()
    val webMercator = crsFactory.createFromName("epsg:3857")
    val wgs84 = crsFactory.createFromName("epsg:4326")
    val ctFactory = CoordinateTransformFactory()
    val mercatorToWgs84: CoordinateTransform = ctFactory.createTransform(webMercator, wgs84)
    val tiePointLatLng =
      mercatorToWgs84.transform(ProjCoordinate(tiePointX, tiePointY), ProjCoordinate())
    val EARTH_CIRC_M = 40075016.686
    val zoomLevel = round(log2(EARTH_CIRC_M * cos(toRadians(tiePointLatLng.y)) / pixelScaleY) - 8.0)

    val tiePointTileX = (tiePointLatLng.x * 2.0.pow(zoomLevel.toDouble()) / 256).toInt()
    val tiePointTileY = (tiePointLatLng.y * 2.0.pow(zoomLevel.toDouble()) / 256).toInt()

    val xTileCount = imageWidth / tileWidth
    val yTileCount = imageLength / tileLength
    // TODO: Verify that tile size is 256x256.
    val idx = x + y * xTileCount
    val jpegTables =
      image
        .getLongListEntryValue(JPEGTables)
        .map(Long::toByte)
        .drop(2) // Skip extraneous SOI.
        .dropLast(2) // Skip extraneous EOI.
        .toByteArray()

    val outdir = File(context.filesDir, "tiles")
    outdir.mkdir()
    val raf = RandomAccessFile(cogFile, "r")
    //    for (idx in offsets.indices) {
    val offset = offsets[idx] + 2 // Skip extraneous SOI
    val len = byteCounts[idx] - 2
    val imageBytes = ByteArray(len.toInt())
    raf.seek(offset)
    raf.read(imageBytes)
    val jpeg =
      START_OF_IMAGE + app0Segment(tileWidth, tileLength) + jpegTables + imageBytes + END_OF_IMAGE
    File(outdir, "tile-$idx.jpg").writeBytes(jpeg)
    //    }
    raf.close()
    // Each overview has its own metadata.
    // Image: 7828x7828
    // Tile: 256x256
    // Image / Tile = 935.021728515625
    // Actual tiles: 31x31 = 961
  }

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

  //    val dir = "/Users/gmiceli/Git/google/ground-android/ground"
  // /data/user/0/com.google.android.ground/files
  // == /data/data/com.google.android.ground/files

  // https://medium.com/planet-stories/a-handy-introduction-to-cloud-optimized-geotiffs-1f2c9e716ec3

  // In my experience, the best format for image serving, using open source rendering engines
  // (MapServer, GeoServer, Mapnik) is: GeoTIFF, with JPEG compression, internally tiled, in the
  // YCBCR color space, with internal overviews.
  // https://blog.cleverelephant.ca/2015/02/geotiff-compression-for-dummies.html
  // Tiles must be in one of these formats:
  // https://developer.android.com/guide/topics/media/media-formats
  // 490x490 LZW
  //    val input =
  // File("/Users/gmiceli/Git/google/ground-android/ground/s2_2022_13_4226_2774.tif")

  // Structure of GeoKeyDirectory:
  // http://geotiff.maptools.org/spec/geotiff2.4.html
  // Dereferencing overview tile is undocumented, as discussed in this report:
  // https://docs.ogc.org/per/21-025.html#toc35

  // Geokey Parameter IDs
  // http://geotiff.maptools.org/spec/geotiff6.html#6.3.

  //    val tiepoint = fullImage.get(FieldTagType.ModelTiepoint)
  //    val origin = tiepoint.values!! as List<Double>
  //    val x = origin[3]
  //    val y = origin[4]
  //    println("Origin: $x, $y")
  //    //  val tilesetExtentZ = 9
  //    // TODO: Check/use TileWidth tag.

  //    println()
  // JPEG compressed tiles don't include headers.
  // https://github.com/vadz/libtiff/blob/master/libtiff/tif_jpeg.c#L1975

  //        val out = ByteArrayOutputStream()
  //    Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).compress(CompressFormat.JPEG, 90,
  // out)
  // Header w/o thumbnail.
  //    val header = out.toByteArray().copyOfRange(0, 17)
  //    val bitmap = BitmapFactory.decodeByteArray(bytes, offsets[0].toInt(),
  // byteCounts[0].toInt())
  //    bitmap.compress(CompressFormat.JPEG, 90, outfile.outputStream())

}

fun byteArrayOf(vararg elements: Int) = elements.map(Int::toByte).toByteArray()

fun Short.toByteArray() = byteArrayOf(this.toInt().shr(8).toByte(), this.toByte())

fun String.toNulTerminatedByteArray() = this.toByteArray() + 0x00.toByte()

fun ByteArray.toHexString() =
  joinToString(" ") {
    Integer.toUnsignedString(java.lang.Byte.toUnsignedInt(it), 16).padStart(2, '0')
  }
