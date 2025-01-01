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
data class MogImageMetadata(
  val originTile: TileCoordinates,
  val tileWidth: Int,
  val tileLength: Int,
  val tileOffsets: List<Long>,
  val byteCounts: List<Long>,
  val imageWidth: Int,
  val imageLength: Int,
  val jpegTables: ByteArray,
  val noDataValue: Int? = null,
) {
  private val tileCountX = imageWidth / tileWidth
  private val tileCountY = imageLength / tileLength

  // TODO: Verify X and Y scales are the same.
  // Issue URL: https://github.com/google/ground-android/issues/2915
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
    require(idx < tileOffsets.size) { "idx >= offsets" }
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

  // Default [equals] and [hashCode] behavior doesn't take array members into account. Defend
  // against accidental usage by throwing exception if called.
  @Suppress("detekt:ExceptionRaisedInUnexpectedLocation")
  override fun equals(other: Any?) = throw UnsupportedOperationException()

  @Suppress("detekt:ExceptionRaisedInUnexpectedLocation")
  override fun hashCode() = throw UnsupportedOperationException()

  companion object {
    fun fromTiffTags(originTile: TileCoordinates, tiffTagToValue: Map<TiffTag, Any?>) =
      MogImageMetadata(
        originTile,
        tiffTagToValue[TiffTag.TileWidth] as Int,
        tiffTagToValue[TiffTag.TileLength] as Int,
        // TODO: Refactor casts into typed accessors.
        // Issue URL: https://github.com/google/ground-android/issues/2915
        tiffTagToValue[TiffTag.TileOffsets] as List<Long>,
        tiffTagToValue[TiffTag.TileByteCounts] as List<Long>,
        tiffTagToValue[TiffTag.ImageWidth] as Int,
        tiffTagToValue[TiffTag.ImageLength] as Int,
        (tiffTagToValue[TiffTag.JPEGTables] as? List<*>)
          ?.map { (it as Int).toByte() }
          ?.toByteArray() ?: byteArrayOf(),
        (tiffTagToValue[TiffTag.GdalNodata] as? String)?.toIntOrNull(),
      )
  }
}
