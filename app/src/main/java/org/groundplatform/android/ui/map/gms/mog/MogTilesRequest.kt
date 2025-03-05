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

package org.groundplatform.android.ui.map.gms.mog

// TODO: Add unit tests.
// Issue URL: https://github.com/google/ground-android/issues/1596
/** A set of [tiles] to be fetched from [sourceUrl] in a single request. */
open class MogTilesRequest(val sourceUrl: String, val tiles: List<MogTileMetadata>) {
  val totalBytes: Int
    get() = tiles.sumOf { it.byteRange.count() }

  val byteRange: LongRange
    get() = LongRange(tiles.first().byteRange.first, tiles.last().byteRange.last)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MogTilesRequest) return false

    if (sourceUrl != other.sourceUrl) return false
    return tiles == other.tiles
  }

  override fun hashCode(): Int {
    var result = sourceUrl.hashCode()
    result = 31 * result + tiles.hashCode()
    return result
  }
}

class MutableMogTilesRequest(sourceUrl: String, tiles: MutableList<MogTileMetadata>) :
  MogTilesRequest(sourceUrl, tiles) {
  fun appendTile(newTile: MogTileMetadata) {
    require(tiles.isEmpty() || tiles.last().byteRange.last < newTile.byteRange.first) {
      "Can't append tile with non-consecutive byte range"
    }
    (tiles as MutableList).add(newTile)
  }

  fun canMergeWith(next: MogTilesRequest, maxOverFetchPerTile: Int) =
    this.sourceUrl == next.sourceUrl &&
      next.byteRange.first - this.byteRange.last - 1 <= maxOverFetchPerTile
}

/**
 * Merges requests for consecutive or near-consecutive byte ranges into a single request to reduce
 * the number of individual HTTP requests required when downloading tiles in bulk for offline use.
 */
fun List<MogTilesRequest>.consolidate(maxOverFetchPerTile: Int): List<MogTilesRequest> {
  val mergedRequests = mutableListOf<MutableMogTilesRequest>()
  for (request in this) {
    for (tile in request.tiles) {
      // Create a new request for the first tile and for each non adjacent tile.
      val lastRequest = mergedRequests.lastOrNull()
      if (lastRequest == null || !lastRequest.canMergeWith(request, maxOverFetchPerTile)) {
        mergedRequests.add(MutableMogTilesRequest(request.sourceUrl, mutableListOf()))
      }
      mergedRequests.last().appendTile(tile)
    }
  }
  return mergedRequests
}
