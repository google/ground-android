package com.google.android.ground.ui.map.gms.cog

data class CogTile(
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