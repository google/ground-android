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

import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.converter.toOfflineAreaEntity
import com.google.android.ground.persistence.local.room.dao.OfflineAreaDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import com.google.android.ground.persistence.local.room.entity.OfflineAreaEntity
import com.google.android.ground.persistence.local.stores.LocalOfflineAreaStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomOfflineAreaStore @Inject internal constructor() : LocalOfflineAreaStore {
  @Inject lateinit var offlineAreaDao: OfflineAreaDao

  override suspend fun insertOrUpdate(area: OfflineArea) =
    offlineAreaDao.insertOrUpdate(area.toOfflineAreaEntity())

  override fun offlineAreas(): Flow<List<OfflineArea>> =
    offlineAreaDao.findAll().map { areas: List<OfflineAreaEntity> ->
      areas.map { it.toModelObject() }
    }

  override suspend fun getOfflineAreaById(id: String): OfflineArea? =
    offlineAreaDao.findById(id)?.toModelObject()

  override suspend fun deleteOfflineArea(offlineAreaId: String) =
    offlineAreaDao.deleteById(offlineAreaId)
}
