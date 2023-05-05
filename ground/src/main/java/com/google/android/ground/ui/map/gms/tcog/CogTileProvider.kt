package com.google.android.ground.ui.map.gms.tcog

import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import timber.log.Timber

/**
 * Implements a Maps SDK [TileProvider] for tiles retrieved from a [TiledCogCollection].
 */
class CogTileProvider(private val tiledCogCollection: TiledCogCollection) : TileProvider {
  override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
    val coords = TileCoordinates(x, y, zoom)
    try {
      val tile = tiledCogCollection.getTile(coords) ?: return null
      return Tile(tile.width.toInt(), tile.height.toInt(), tile.imageBytes)
    } catch (e: Throwable) {
      Timber.d(e, "Failed to fetch tile at $coords")
      return null
    }
  }
}
