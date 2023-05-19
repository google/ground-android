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

class MogTileIndex(
  val ifd: MogImageMetadata,
  // TODO: Replace with LongRange
  val tileOffsets: List<Long>,
  val byteCounts: List<Long>
) {

  fun getByteRange(x: Int, y: Int): LongRange? {
    // TODO: Check actual length of arrays as well.
    if (!ifd.hasTile(x, y)) return null
    val xIdx = x - ifd.originTile.x
    val yIdx = y - ifd.originTile.y
    val idx = yIdx * ifd.tileCountX + xIdx
    if (idx > tileOffsets.size) throw IllegalArgumentException("idx > offsets")
    val from = tileOffsets[idx]
    val len = byteCounts[idx].toInt()
    val to = from + len - 1
    return from..to
  }
}
