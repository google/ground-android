package com.google.android.ground.ui.map.gms.cog

import android.content.Context
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffReader
import java.io.File

class CogTileProvider(@ApplicationContext private val context: Context) : TileProvider {
  private val cog: Cog

  init {
    val cogFile = File(context.filesDir, "cogs/9/391/248.tif")
    val tiff: TIFFImage = TiffReader.readTiff(cogFile)
    cog = Cog(cogFile, tiff)
  }

  override fun getTile(x: Int, y: Int, z: Int): Tile? {
    val cogTile = cog.getTile(x, y, z) ?: return null
    return Tile(cogTile.width.toInt(), cogTile.height.toInt(), cogTile.imageBytes)
  }
}