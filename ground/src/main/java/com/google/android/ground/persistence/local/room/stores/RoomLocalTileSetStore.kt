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
package com.google.android.ground.persistence.local.room.stores

import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.dao.TileSetDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import com.google.android.ground.persistence.local.room.entity.TileSetEntity
import com.google.android.ground.persistence.local.room.models.TileSetEntityState
import com.google.android.ground.persistence.local.stores.LocalTileSetStore
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.ui.util.FileUtil
import com.google.android.ground.util.toImmutableList
import com.google.android.ground.util.toImmutableSet
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomLocalTileSetStore @Inject internal constructor(): LocalTileSetStore {
  @Inject lateinit var tileSetDao: TileSetDao
  @Inject lateinit var schedulers: Schedulers
  @Inject lateinit var fileUtil: FileUtil

  override val tileSetsOnceAndStream: Flowable<ImmutableSet<TileSet>>
    get() =
      tileSetDao
        .findAllOnceAndStream()
        .map { list: List<TileSetEntity> -> list.map { it.toModelObject() }.toImmutableSet() }
        .subscribeOn(schedulers.io())

  override fun insertOrUpdateTileSet(tileSet: TileSet): Completable =
    tileSetDao.insertOrUpdate(tileSet.toLocalDataStoreObject()).subscribeOn(schedulers.io())

  override fun getTileSet(tileUrl: String): Maybe<TileSet> =
    tileSetDao.findByUrl(tileUrl).map { it.toModelObject() }.subscribeOn(schedulers.io())

  override val pendingTileSets: Single<ImmutableList<TileSet>>
    get() =
      tileSetDao
        .findByState(TileSetEntityState.PENDING.intValue())
        .map { list: List<TileSetEntity> -> list.map { it.toModelObject() }.toImmutableList() }
        .subscribeOn(schedulers.io())

  override fun updateTileSetOfflineAreaReferenceCountByUrl(
    newCount: Int,
    url: String
  ): Completable = Completable.fromSingle(tileSetDao.updateBasemapReferenceCount(newCount, url))

  override fun deleteTileSetByUrl(tileSet: TileSet): Completable =
    if (tileSet.offlineAreaReferenceCount < 1) {
      Completable.fromAction { fileUtil.deleteFile(tileSet.path) }
        .andThen(Completable.fromMaybe(tileSetDao.deleteByUrl(tileSet.url)))
        .subscribeOn(schedulers.io())
    } else {
      Completable.complete().subscribeOn(schedulers.io())
    }
}
