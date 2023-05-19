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

/** A set of [tiles] to be fetched from [sourceUrl] in a single request. */
data class MogTilesRequest(val sourceUrl: String, val tiles: List<MogTileMetadata>)

class MutableMogTilesRequest(var sourceUrl: String, val tiles: MutableList<MogTileMetadata>) {
  fun toTilesRequest() = MogTilesRequest(sourceUrl, tiles)

  fun appendTiles(newTiles: List<MogTileMetadata>) {
//    require(this.sourceUrl == other.sourceUrl) {
//      "Can't append tiles  with different source URLs"
//    }
    require(this.tiles.last().byteRange.last < newTiles.first().byteRange.first) {
      "Can't append tile  with non-consecutive and non-overlapping byte range"
    }
    this.tiles.addAll(newTiles)
  }
}
