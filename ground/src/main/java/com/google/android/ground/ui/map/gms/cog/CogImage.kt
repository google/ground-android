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

class CogImage(
  val originTile: TileCoordinates,
  val offsets: List<Long>,
  val byteCounts: List<Long>,
  val tileWidth: Short,
  val tileLength: Short,
  val imageWidth: Short,
  val imageLength: Short,
  // TODO: Use ByteArray instead?
  val jpegTables: List<Byte>?
) {
  val tileCountX = imageWidth / tileWidth
  val tileCountY = imageLength / tileLength

  // TODO: Verify X and Y scales the same.
  //  val tiePointLatLng = CoordinateTransformer.webMercatorToWgs84(tiePointX, tiePointY)
  val zoomLevel = originTile.zoomLevel // zoomLevelFromScale(pixelScaleY, tiePointLatLng.latitude)

  fun hasTile(coordinates: TileCoordinates): Boolean {
    val (x, y, zoom) = coordinates
    return zoom == zoomLevel && hasTile(x, y)
  }

  private fun hasTile(x: Int, y: Int) =
    x >= originTile.x &&
      y >= originTile.y &&
      x < tileCountX + originTile.x &&
      y < tileCountY + originTile.y

  override fun toString(): String {
    return "CogImage(originTile=$originTile, offsets=.., byteCounts=.., tileWidth=$tileWidth, tileLength=$tileLength, imageWidth=$imageWidth, imageLength=$imageLength, tileCountX=$tileCountX, tileCountY=$tileCountY, jpegTablesBody=.., zoomLevel=$zoomLevel)"
  }

  fun getByteRange(x: Int, y: Int): LongRange? {
    if (!hasTile(x, y)) return null
    val xIdx = x - originTile.x
    val yIdx = y - originTile.y
    val idx = yIdx * tileCountX + xIdx
    if (idx > offsets.size) throw IllegalArgumentException("idx > offsets")
    val from = offsets[idx] + 2 // Skip extraneous SOI
    val len = byteCounts[idx].toInt() - 2
    val to = from + len - 1
    return from..to
  }
}
