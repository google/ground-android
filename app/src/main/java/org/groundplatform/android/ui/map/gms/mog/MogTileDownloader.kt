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

import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Downloads tiles across regions at multiple zoom levels.
 *
 * @param client the client to be used for fetching metadata and tiles..
 * @param outputBasePath the base path on the local file system where tiles should be written.
 */
class MogTileDownloader(private val client: MogClient, private val outputBasePath: String) {
  // TODO: Add test coverage to this class and entire package.
  // Issue URL: https://github.com/google/ground-android/issues/1755
  /**
   * Executes the provided [requests], writing resulting tiles to [outputBasePath] in sub-paths of
   * the form `{z}/{x}/{y}.jpg`.
   */
  suspend fun downloadTiles(requests: List<MogTilesRequest>) =
    flow {
        client.getTiles(requests).collect { tile ->
          currentCoroutineContext().ensureActive()
          val outFile = File(outputBasePath, tile.metadata.tileCoordinates.getTilePath())
          outFile.parentFile?.mkdirs()
          val gmsTile = tile.toGmsTile()
          val data = gmsTile.data!!
          outFile.writeBytes(data)
          Timber.d("Saved ${data.size} bytes to ${outFile.path}")
          emit(data.size)
        }
      }
      .cancellable()
}

fun TileCoordinates.getTilePath() = "$zoom/$x/$y.jpg"
