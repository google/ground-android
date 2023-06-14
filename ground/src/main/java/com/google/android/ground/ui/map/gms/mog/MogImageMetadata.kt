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

package com.google.android.ground.ui.map.gms.mog

/** Metadata describing a single full-resolution or overview image in a MOG. */
@Suppress("MemberVisibilityCanBePrivate")
class MogImageMetadata(
  val tileWidth: Int,
  val tileLength: Int,
  val originTile: TileCoordinates,
  val tileOffsets: List<Long>,
  val byteCounts: List<Long>,
  val imageWidth: Int,
  val imageLength: Int,
  val jpegTables: ByteArray
) {
  val tileCountX = imageWidth / tileWidth
  val tileCountY = imageLength / tileLength

  // TODO: Verify X and Y scales are the same.
  val zoom = originTile.zoom

  fun hasTile(x: Int, y: Int) =
    x >= originTile.x &&
      y >= originTile.y &&
      x < tileCountX + originTile.x &&
      y < tileCountY + originTile.y

  fun getByteRange(x: Int, y: Int): LongRange? {
    if (!hasTile(x, y)) return null
    val xIdx = x - originTile.x
    val yIdx = y - originTile.y
    val idx = yIdx * tileCountX + xIdx
    require(idx <= tileOffsets.size) { "idx > offsets" }
    val from = tileOffsets[idx]
    val len = byteCounts[idx].toInt()
    val to = from + len - 1
    return from..to
  }

  override fun toString(): String =
    "MogImage(" +
      "originTile=$originTile, " +
      "offsets=.., " +
      "byteCounts=.., " +
      "tileWidth=$tileWidth, " +
      "tileLength=$tileLength, " +
      "imageWidth=$imageWidth, " +
      "imageLength=$imageLength, " +
      "tileCountX=$tileCountX, " +
      "tileCountY=$tileCountY, " +
      "jpegTables=.., " +
      "zoom=$zoom)"
}
