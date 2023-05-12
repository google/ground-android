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

import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class MogTileProvider(private val collection: MogCollection) : TileProvider {
  private val client = MogClient(collection)

  override fun getTile(x: Int, y: Int, zoom: Int): Tile? = runBlocking {
    val tileCoordinates = TileCoordinates(x, y, zoom)
    try {
      client.getTile(tileCoordinates)
      //      mogCollection.applyMask(tile, tileCoordinates)
    } catch (e: Throwable) {
      // Maps SDK doesn't log exceptions thrown by [getTile] implementations, so we log them here.
      Timber.d(e, "Error fetching tile at $tileCoordinates")
      null
    }
  }
}
