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
package com.google.android.ground.persistence.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.google.android.ground.persistence.local.room.entity.TileSetEntity
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface TileSetDao : BaseDao<TileSetEntity> {
  @Query("SELECT * FROM tile_sources") fun findAllOnceAndStream(): Flowable<List<TileSetEntity>>

  @Query("SELECT * FROM tile_sources WHERE state = :state")
  fun findByState(state: Int): Single<List<TileSetEntity>>

  @Query("SELECT * FROM tile_sources WHERE id = :id") fun findById(id: String): Maybe<TileSetEntity>

  @Query("SELECT * FROM tile_sources WHERE url = :url")
  fun findByUrl(url: String): Maybe<TileSetEntity>

  @Query("SELECT * FROM tile_sources WHERE path = :path")
  fun findByPath(path: String): Maybe<TileSetEntity>

  @Query("UPDATE tile_sources SET basemap_count=:newCount WHERE url = :url")
  fun updateBasemapReferenceCount(newCount: Int, url: String): Single<Int>

  @Query("DELETE FROM tile_sources WHERE url = :url") fun deleteByUrl(url: String): Maybe<Int>
}
