package com.google.android.ground.ui.map.gms.mog

/*
 * Copyright 2018 Google LLC
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

/** A contiguous range of bytes in a MOG and coordinates of tiles of interest stored therein. */
data class MogTileRange(
  /**
   * The start and end index of the relevant bytes in the MOG. Indices start from 0. End index is
   * inclusive.
   */
  val byteRange: LongRange,

  /**
   * Relevant list of coordinates of the web mercator tile(s) referenced by [byteRange]. Tiles
   * located within the range but not included in this list are ignored or skipped. The exact number
   * of bytes occupied by each tile is defined in the image source's headers.
   */
  val tileCoordinatesList: List<TileCoordinates>
)

data class MutableTilesRequest(
  /**
   * The start and end index of the relevant bytes in the MOG. Indices start from 0. End index is
   * inclusive.
   */
  var byteRange: LongRange,

  /**
   * Relevant list of coordinates of the web mercator tile(s) referenced by [byteRange]. Tiles
   * located within the range but not included in this list are ignored or skipped. The exact number
   * of bytes occupied by each tile is defined in the image source's headers.
   */
  val tileCoordinatesList: MutableList<TileCoordinates>
) {
  fun toTilesRequest() = MogTileRange(byteRange, tileCoordinatesList)

  /**
   * Adds an additional tile to be fetched by extending the number of bytes requested and its
   * corresponding tile coordinates.
   */
  fun extendRange(newEnd: Long, newTileCoordinates: TileCoordinates) {
    byteRange = LongRange(byteRange.first, newEnd)
    tileCoordinatesList.add(newTileCoordinates)
  }
}
