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
import java.lang.Math.*
import javax.inject.Inject
import mil.nga.tiff.FieldTagType.*
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffReader
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
    val x = 251
    val y = 391
    val z = 9

    //
    val cogFile = File(context.filesDir, "cogs/9/391/251.tif")
    val tiff: TIFFImage = TiffReader.readTiff(cogFile)
    //    val cog = CogHeaders(cogFile, tiff)
    //    val image = cog.getTile(x, y, z)
    //    val image = cog.imagesByZoomLevel[z] ?: return
    //    val idx = image.tileIndex(x, y)
    //    val outdir = File(context.filesDir, "tiles")
    //    outdir.mkdir()
    //    val raf = RandomAccessFile(cogFile, "r")
    //    val offset = image.offsets[idx] + 2 // Skip extraneous SOI
    //    val len = image.byteCounts[idx] - 2
    //    val imageBytes = ByteArray(len.toInt())
    //    raf.seek(offset)
    //    raf.read(imageBytes)
    //    val jpeg = image.buildJpegTile(imageBytes)
    //    File(outdir, "tile-$idx.jpg").writeBytes(jpeg)
    //    raf.close()
    //  }

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
}
// fun ByteArray.toHexString() =
//  joinToString(" ") {
//    Integer.toUnsignedString(java.lang.Byte.toUnsignedInt(it), 16).padStart(2, '0')
//  }
