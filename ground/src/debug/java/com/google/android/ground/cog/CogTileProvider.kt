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

import android.content.Context
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffReader

class CogTileProvider(@ApplicationContext private val context: Context) : TileProvider {
  private val cog: Cog

  init {
    val cogFile = File(context.filesDir, "cogs/9/391/251.tif")
    val tiff: TIFFImage = TiffReader.readTiff(cogFile)
    cog = Cog(cogFile, tiff)
  }

  override fun getTile(x: Int, y: Int, z: Int): Tile? {
    val cogTile = cog.getTile(x, y, z) ?: return null
    return Tile(cogTile.width.toInt(), cogTile.height.toInt(), cogTile.imageBytes)
  }
}
