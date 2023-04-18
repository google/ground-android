package com.google.android.ground.ui.map.gms.cog

import android.content.Context
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import dagger.hilt.android.qualifiers.ApplicationContext

class CogTileProvider(@ApplicationContext private val context: Context) : TileProvider {
  private val cogs = CogCollection(context.filesDir.path + "/cogs/{z}/{x}/{y}.tif", 9)

  override fun getTile(x: Int, y: Int, z: Int): Tile? {
    val cogTile = cogs.getTile(x, y, z) ?: return null
    return Tile(cogTile.width.toInt(), cogTile.height.toInt(), cogTile.imageBytes)
  }
}
