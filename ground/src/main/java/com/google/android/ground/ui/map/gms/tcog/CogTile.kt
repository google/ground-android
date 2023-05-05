package com.google.android.ground.ui.map.gms.tcog

data class CogTile(
  val coordinates: TileCoordinates,
  val width: Short,
  val height: Short,
  val imageBytes: ByteArray
) {
  override fun equals(other: Any?): Boolean {
    throw NotImplementedError()
  }
//tileWidth, tileLength, buildJpegTile(imageBytes)
  override fun hashCode(): Int {
    throw NotImplementedError()
  }
}