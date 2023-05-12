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

import java.io.File

/**
 * Downloads tiles across regions at multiple zoom levels.
 *
 * @param mogCollection the collection from which tiles will be fetched.
 * @param outputBasePath the base path on the local file system where tiles should be written.
 */
class MogTileDownloader(private val client: MogClient, private val outputBasePath: String) {
  /**
   * Executes the provided [tilesRequests], writing resulting tiles to [outputBasePath] in sub=paths
   * of the form `{z}/{x}/{y}.jpg`.
   */
  suspend fun downloadTiles(tilesRequests: List<TilesRequest>) {
    client.getTiles(tilesRequests).collect { (coordinates, tile) ->
      val (x, y, zoom) = coordinates
      val path = File(outputBasePath, "$zoom/$x")
      path.mkdirs()
      File(path, "$y.jpg").writeBytes(tile.data!!)
    }
  }
}
