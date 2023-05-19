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

import java.io.InputStream
import timber.log.Timber

/** Metadata describing a single full-resolution or overview image in a MOG. */
@Suppress("MemberVisibilityCanBePrivate")
class MogImageMetadata(
  val tileWidth: Int,
  val tileLength: Int,
  val originTile: TileCoordinates,
  val tileOffsetsByteRange: LongRange,
  val byteCountsByteRange: LongRange,
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

  override fun toString(): String {
    return "MogImage(originTile=$originTile, offsets=.., byteCounts=.., tileWidth=$tileWidth, tileLength=$tileLength, imageWidth=$imageWidth, imageLength=$imageLength, tileCountX=$tileCountX, tileCountY=$tileCountY, jpegTables=.., zoom=$zoom)"
  }

}
