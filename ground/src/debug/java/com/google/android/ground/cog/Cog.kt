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
import mil.nga.tiff.TIFFImage

class Cog(private val cogFile: File, private val tiff: TIFFImage) {

  fun getTile(x: Int, y: Int, z: Int): CogTile? = imagesByZoomLevel[z]?.getTile(x, y)

  val imagesByZoomLevel: Map<Int, CogImage>

  init {
    val images = mutableListOf<CogImage>()
    // IFDs are in decreasing detail (decreasing zoom), starting with max, ending with min zoom.
    for (ifd in tiff.fileDirectories) {
      if (images.isEmpty()) {
        // First image is full-size image and defines pixel scale.
        images.add(CogImage(cogFile, ifd))
      } else {
        // Each successive overview has 2x the pixel scale of the previous image.
        val prev = images.last()
        images.add(
          CogImage(
            cogFile,
            ifd,
            prev.pixelScaleX * 2,
            prev.pixelScaleY * 2,
            prev.tiePointX,
            prev.tiePointY
          )
        )
      }
    }
    imagesByZoomLevel = images.map { it.zoomLevel to it }.toMap()
  }
}
