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

/** A request for one or more tiles from a particular file. */
data class TilesRequest(
  /** The URL of the source MOG. */
  val mogUrl: String,

  /** The web mercator tile coordinates corresponding to the bounding box of the source MOG. */
  val mogBounds: TileCoordinates,

  /**
   * The start and end index of the relevant bytes to be fetched from the source image. Indices
   * start from 0. End index is inclusive.
   */
  val byteRange: LongRange,

  /**
   * The list of coordinates of the web mercator tile(s) being requested. The exact number of bytes
   * in [byteRange] occupied by each tile is defined in the image source's headers.
   */
  val tileCoordinatesList: List<TileCoordinates>
)

data class MutableTilesRequest(
  /** The URL of the source MOG. */
  var mogUrl: String,

  /** The web mercator tile coordinates corresponding to the bounding box of the source MOG. */
  var mogBounds: TileCoordinates,

  /**
   * The start and end index of the relevant bytes to be fetched from the source image. Indices
   * start from 0. End index is inclusive.
   */
  var byteRange: LongRange,

  /**
   * The list of coordinates of the web mercator tile(s) being requested. The exact number of bytes
   * in [byteRange] occupied by each tile is defined in the image source's headers.
   */
  val tileCoordinatesList: MutableList<TileCoordinates>
) {
  fun toTilesRequest() = TilesRequest(mogUrl, mogBounds, byteRange, tileCoordinatesList)

  /**
   * Adds an additional tile to be fetched by extending the number of bytes requested and its
   * corresponding tile coordinates.
   */
  fun extendRange(newEnd: Long, newTileCoordinates: TileCoordinates) {
    byteRange = LongRange(byteRange.first, newEnd)
    tileCoordinatesList.add(newTileCoordinates)
  }
}
