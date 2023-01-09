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
package com.google.android.ground.persistence.local.stores

import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.rx.annotations.Cold
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

interface LocalTileSetStore : LocalStore<TileSet> {
  /**
   * Returns a long-lived stream that emits the full set of tiles on subscribe and continues to
   * return the full set each time a tile is added/changed/removed.
   */
  val tileSetsOnceAndStream: @Cold(terminates = false) Flowable<ImmutableSet<TileSet>>

  /**
   * Attempts to update a tile in the local data store. If the tile doesn't exist, inserts the tile
   * into the local data store.
   */
  fun insertOrUpdateTileSet(tileSet: TileSet): @Cold Completable

  /** Returns the tile with the specified URL from the local data store, if found. */
  fun getTileSet(tileUrl: String): @Cold Maybe<TileSet>

  /** Returns all pending tiles from the local data store. */
  val pendingTileSets: @Cold Single<ImmutableList<TileSet>>

  /**
   * Update the area count of an existing tile source in the local data store with the area count of
   * [TileSet].
   */
  fun updateTileSetOfflineAreaReferenceCountByUrl(newCount: Int, url: String): @Cold Completable

  /** Delete a tile source associated with a given URL from the local data store. */
  fun deleteTileSetByUrl(tileSet: TileSet): @Cold Completable
}
