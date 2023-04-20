package com.google.android.ground.ui.map.gms.cog

import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider

class CogTileProvider(private val cogCollection: CogCollection) : TileProvider {
  override fun getTile(x: Int, y: Int, z: Int): Tile? {
    val tile = cogCollection.getTile(TileCoordinates(x, y, z)) ?: return null
    return Tile(tile.width.toInt(), tile.height.toInt(), tile.imageBytes)
  }
}
