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

import java.net.HttpURLConnection
import java.net.URL
import mil.nga.tiff.FieldTagType.*
import mil.nga.tiff.TiffReader
import timber.log.Timber
import java.lang.System.currentTimeMillis

/**
 * Implementation of [CogProvider] using the [NGA TIFF Java](https://github.com/ngageoint/tiff-java)
 * library to parse TIFF headers.
 */
class NgaCogProvider : CogProvider {
  override fun getCog(url: URL, extent: TileCoordinates): Cog? {
    val startTimeMillis = currentTimeMillis()
    // TODO: Refactor with similar logic in CogCollection.
    val urlConnection = url.openConnection() as HttpURLConnection
    urlConnection.requestMethod = "GET"
    urlConnection.connect()
    val inputStream = urlConnection.inputStream
    try {
      val responseCode = urlConnection.responseCode
      if (responseCode == 404 || responseCode != 200) {
        Timber.d("Failed to load COG headers. HTTP $responseCode on $url")
        return null
      }
      // This reads only headers and not the whole file.
      val tiff = TiffReader.readTiff(inputStream)
      val images = mutableListOf<CogImage>()
      // IFDs are in decreasing detail (decreasing zoom), starting with max, ending with min zoom.
      val maxZ = extent.zoom + tiff.fileDirectories.size - 1
      tiff.fileDirectories.forEachIndexed { i, ifd ->
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
            ifd.getLongListEntryValue(JPEGTables).map(Long::toByte)
          )
        )
      }
      val time = currentTimeMillis() - startTimeMillis
      Timber.d("Loaded COG headers in $time ms from $url")
      return Cog(extent, images.toList())
    } finally {
      inputStream.close()
      urlConnection.disconnect()
    }
  }
}
