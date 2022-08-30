/*
 * Copyright 2019 Google LLC
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
package com.google.android.ground.model.basemap.tile

/** Represents a source of offline imagery tileset data. */
data class TileSet(
  val url: String,
  val id: String,
  val path: String,
  val state: State,
  val offlineAreaReferenceCount: Int
) {
  /** Increment the area reference count of a tile source by one. */
  fun incrementOfflineAreaCount(): TileSet =
    copy(offlineAreaReferenceCount = offlineAreaReferenceCount + 1)

  /** Decrement the area reference count of a tile source by one. */
  fun decrementOfflineAreaCount(): TileSet =
    copy(offlineAreaReferenceCount = offlineAreaReferenceCount - 1)

  enum class State {
    PENDING,
    IN_PROGRESS,
    DOWNLOADED,
    FAILED
  }

  companion object {
    @JvmStatic
    fun pathFromId(tileSetId: String): String {
      // Tile ids are stored as x-y-z. Paths must be z-x-y.mbtiles.
      // TODO: Convert tile ids to paths in a less restrictive and less hacky manner.
      // TODO: Move this method to a more appropriate home? We need to perform (and possibly
      //  will no matter where the tiles are stored) translation between the tile ID and the
      //  file path of the corresponding tile source in remote storage/wherever we pull the
      //  source tile from.
      val fields = tileSetId.replace(Regex("[()]"), "").split(", ")
      val filename = fields[2] + "-" + fields[0] + "-" + fields[1]
      return "$filename.mbtiles"
    }
  }
}
