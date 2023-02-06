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

import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.converter.toOfflineAreaEntity
import com.google.android.ground.persistence.local.room.dao.OfflineAreaDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import com.google.android.ground.persistence.local.room.entity.OfflineAreaEntity
import com.google.android.ground.persistence.local.stores.LocalOfflineAreaStore
import com.google.android.ground.rx.Schedulers
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class RoomOfflineAreaStore @Inject internal constructor() : LocalOfflineAreaStore {
  @Inject lateinit var offlineAreaDao: OfflineAreaDao
  @Inject lateinit var schedulers: Schedulers

  override fun insertOrUpdateOfflineArea(area: OfflineArea): Completable =
    offlineAreaDao.insertOrUpdate(area.toOfflineAreaEntity()).subscribeOn(schedulers.io())

  override val offlineAreasOnceAndStream: Flowable<List<OfflineArea>>
    get() =
      offlineAreaDao
        .findAllOnceAndStream()
        .map { areas: List<OfflineAreaEntity> -> areas.map { it.toModelObject() } }
        .subscribeOn(schedulers.io())

  override fun getOfflineAreaById(id: String): Single<OfflineArea> =
    offlineAreaDao.findById(id).map { it.toModelObject() }.toSingle().subscribeOn(schedulers.io())

  override fun deleteOfflineArea(offlineAreaId: String): Completable =
    offlineAreaDao
      .findById(offlineAreaId)
      .toSingle()
      .doOnSubscribe { Timber.d("Deleting offline area: $offlineAreaId") }
      .flatMapCompletable { offlineAreaDao.delete(it) }
      .subscribeOn(schedulers.io())
}
