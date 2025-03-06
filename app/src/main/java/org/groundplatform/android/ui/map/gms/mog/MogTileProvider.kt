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

import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import kotlinx.coroutines.runBlocking
import org.groundplatform.android.persistence.remote.RemoteStorageManager
import timber.log.Timber

// TODO: Add unit tests.
// Issue URL: https://github.com/google/ground-android/issues/1596
/** Fetches and returns MOG tiles to Maps SDK for display as a tile overlay. */
class MogTileProvider(collection: MogCollection, remoteStorageManager: RemoteStorageManager) :
  TileProvider {
  private val client = MogClient(collection, remoteStorageManager)

  override fun getTile(x: Int, y: Int, zoom: Int): Tile? = runBlocking {
    val tileCoordinates = TileCoordinates(x, y, zoom)
    try {
      client.getTile(tileCoordinates)?.toGmsTile()
    } catch (e: Throwable) {
      // Maps SDK doesn't log exceptions thrown by [TileProvider] implementations, so we do it here.
      Timber.d(e, "Error fetching tile at $tileCoordinates")
      null
    }
  }
}
