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

package com.google.android.gnd.persistence.local.room.dao;

import androidx.room.Dao;
import androidx.room.Query;
import com.google.android.gnd.persistence.local.room.entity.TileSetEntity;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;

@Dao
public interface TileSetDao extends BaseDao<TileSetEntity> {

  @Query("SELECT * FROM tile_sources")
  Flowable<List<TileSetEntity>> findAllOnceAndStream();

  @Query("SELECT * FROM tile_sources WHERE state = :state")
  Single<List<TileSetEntity>> findByState(int state);

  @Query("SELECT * FROM tile_sources WHERE id = :id")
  Maybe<TileSetEntity> findById(String id);

  @Query("SELECT * FROM tile_sources WHERE url = :url")
  Maybe<TileSetEntity> findByUrl(String url);

  @Query("SELECT * FROM tile_sources WHERE path = :path")
  Maybe<TileSetEntity> findByPath(String path);

  @Query("UPDATE tile_sources SET basemap_count=:newCount WHERE url = :url")
  Single<Integer> updateBasemapReferenceCount(int newCount, String url);

  @Query("DELETE FROM tile_sources WHERE url = :url")
  Maybe<Integer> deleteByUrl(String url);
}
