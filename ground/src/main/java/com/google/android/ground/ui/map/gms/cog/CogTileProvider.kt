package com.google.android.ground.ui.map.gms.cog

import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import timber.log.Timber

class CogTileProvider(private val cogCollection: CogCollection) : TileProvider {
  override fun getTile(x: Int, y: Int, z: Int): Tile? {
    val coords = TileCoordinates(x, y, z)
    try {
      val tile = cogCollection.getTile(coords) ?: return null
      return Tile(tile.width.toInt(), tile.height.toInt(), tile.imageBytes)
    } catch (e: Throwable) {
      Timber.d(e, "Failed to fetch tile at $coords")
      return null
    }
  }
}
