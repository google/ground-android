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

import com.google.android.ground.model.imagery.MbtilesFile
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.dao.MbtilesFileDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdateSuspend
import com.google.android.ground.persistence.local.room.entity.MbtilesFileEntity
import com.google.android.ground.persistence.local.room.fields.TileSetEntityState
import com.google.android.ground.persistence.local.stores.LocalTileSetStore
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.ui.util.FileUtil
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTileSetStore @Inject internal constructor() : LocalTileSetStore {
  @Inject lateinit var mbtilesFileDao: MbtilesFileDao
  @Inject lateinit var schedulers: Schedulers
  @Inject lateinit var fileUtil: FileUtil

  override fun tileSetsOnceAndStream(): Flowable<Set<MbtilesFile>> =
    mbtilesFileDao
      .findAllOnceAndStream()
      .map { list: List<MbtilesFileEntity> -> list.map { it.toModelObject() }.toSet() }
      .subscribeOn(schedulers.io())

  override suspend fun insertOrUpdateTileSet(mbtilesFile: MbtilesFile) =
    mbtilesFileDao.insertOrUpdateSuspend(mbtilesFile.toLocalDataStoreObject())

  override fun getTileSet(tileUrl: String): Maybe<MbtilesFile> =
    mbtilesFileDao.findByUrl(tileUrl).map { it.toModelObject() }.subscribeOn(schedulers.io())

  override suspend fun pendingTileSets(): List<MbtilesFile> =
    mbtilesFileDao.findByState(TileSetEntityState.PENDING.intValue())?.map { it.toModelObject() }
      ?: listOf()

  override fun updateTileSetOfflineAreaReferenceCountByUrl(
    newCount: Int,
    url: String
  ): Completable = Completable.fromSingle(mbtilesFileDao.updateReferenceCount(newCount, url))

  override fun deleteTileSetByUrl(mbtilesFile: MbtilesFile): Completable =
    if (mbtilesFile.referenceCount < 1) {
      Completable.fromAction { fileUtil.deleteFile(mbtilesFile.path) }
        .andThen(Completable.fromMaybe(mbtilesFileDao.deleteByUrl(mbtilesFile.url)))
        .subscribeOn(schedulers.io())
    } else {
      Completable.complete().subscribeOn(schedulers.io())
    }
}
