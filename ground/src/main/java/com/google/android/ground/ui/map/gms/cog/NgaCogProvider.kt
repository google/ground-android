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
import mil.nga.tiff.FieldTagType.*
import mil.nga.tiff.TiffReader

/**
 * Implementation of [CogProvider] using the [NGA TIFF Java](https://github.com/ngageoint/tiff-java)
 * library to parse TIFF headers.
 */
class NgaCogProvider : CogProvider {
  override fun getCog(file: File, extent: TileCoordinates): Cog? {
    // TODO: Read over HTTP.
    if (!file.exists()) return null
    // TODO: Verifying NGA reads only headers and not the whole file.
    val tiff = TiffReader.readTiff(file)
    val images = mutableListOf<CogImage>()
    // IFDs are in decreasing detail (decreasing zoom), starting with max, ending with min zoom.
    val maxZ = extent.z + tiff.fileDirectories.size - 1
    tiff.fileDirectories.forEachIndexed { i, ifd ->
      images.add(
        CogImage(
          file,
          extent.originTileAtZoom(maxZ - i),
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
    return Cog(extent, images.toList())
  }
}
