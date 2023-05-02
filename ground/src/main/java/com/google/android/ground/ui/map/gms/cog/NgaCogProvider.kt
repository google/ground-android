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

import java.lang.System.currentTimeMillis
import java.net.HttpURLConnection
import java.net.URL
import mil.nga.tiff.FieldTagType.*
import mil.nga.tiff.TiffReader
import mil.nga.tiff.util.TiffConstants.PHOTOMETRIC_INTERPRETATION_RGB
import mil.nga.tiff.util.TiffException
import timber.log.Timber

/**
 * Implementation of [CogProvider] using the [NGA TIFF Java](https://github.com/ngageoint/tiff-java)
 * library to parse TIFF headers.
 */
class NgaCogProvider : CogProvider {
  override fun getCog(url: URL, extent: TileCoordinates): Cog {
    val startTimeMillis = currentTimeMillis()
    // TODO: Refactor with similar logic in CogCollection.
    val urlConnection = url.openConnection() as HttpURLConnection
    urlConnection.requestMethod = "GET"
    urlConnection.connect()
    val inputStream = urlConnection.inputStream
    try {
      val responseCode = urlConnection.responseCode
      if (responseCode == 404 || responseCode != 200) {
        throw CogException("Failed to load COG headers. HTTP $responseCode on $url")
      }
      // This reads only headers and not the whole file.
      val tiff = TiffReader.readTiff(inputStream)
      val images = mutableListOf<CogImage>()
      // Only include image file directories with RGB image data. Mask images are skipped.
      // TODO: Use masks to render areas with no data as transparent.
      val rgbIfds =
        tiff.fileDirectories.filter {
          it.getIntegerEntryValue(PhotometricInterpretation).and(PHOTOMETRIC_INTERPRETATION_RGB) !=
            0
        }
      // IFDs are in decreasing detail (decreasing zoom), starting with max, ending with min zoom.
      val maxZ = extent.zoom + rgbIfds.size - 1
      rgbIfds.forEachIndexed { i, ifd ->
        images.add(
          CogImage(
            url,
            extent.originAtZoom(maxZ - i),
            ifd.getLongListEntryValue(TileOffsets),
            ifd.getLongListEntryValue(TileByteCounts),
            ifd.getIntegerEntryValue(TileWidth).toShort(),
            ifd.getIntegerEntryValue(TileLength).toShort(),
            ifd.getIntegerEntryValue(ImageWidth).toShort(),
            ifd.getIntegerEntryValue(ImageLength).toShort(),
            ifd.getLongListEntryValue(JPEGTables)?.map(Long::toByte)
          )
        )
      }
      val time = currentTimeMillis() - startTimeMillis
      Timber.d("Loaded COG headers in $time ms from $url")
      return Cog(extent, images.toList())
    } catch (e: TiffException) {
      throw CogException("Failed to read COG: ${e.message})")
    } finally {
      inputStream.close()
      urlConnection.disconnect()
    }
  }
}
