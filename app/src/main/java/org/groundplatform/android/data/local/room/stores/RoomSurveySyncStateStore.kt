/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.data.local.room.stores

import javax.inject.Inject
import kotlin.time.Clock
import org.groundplatform.android.data.local.room.converter.toModelObject
import org.groundplatform.android.data.local.room.dao.SurveySyncStateDao
import org.groundplatform.android.data.local.room.dao.insertOrUpdate
import org.groundplatform.android.data.local.room.entity.SurveySyncStateEntity
import org.groundplatform.android.data.local.stores.LocalSurveySyncStateStore
import org.groundplatform.android.data.remote.firebase.protobuf.toProto
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveySyncState

class RoomSurveySyncStateStore
@Inject
constructor(private val surveySyncStateDao: SurveySyncStateDao) : LocalSurveySyncStateStore {
  override suspend fun get(surveyId: String): SurveySyncState? {
    val entity = surveySyncStateDao.get(surveyId)
    return entity?.toModelObject()
  }

  override suspend fun recordIncrementalSync(
    surveyId: String,
    latestLoiServerTimestamp: Long,
  ) {
    surveySyncStateDao.updateLatestLoiServerTimestamp(surveyId, latestLoiServerTimestamp)
  }

  override suspend fun recordFullSync(
    surveyId: String,
    latestLoiServerTimestamp: Long,
    dataVisibility: Survey.DataVisibility?,
  ) {
    surveySyncStateDao.insertOrUpdate(
      SurveySyncStateEntity(
        surveyId = surveyId,
        latestLoiServerTimestamp = latestLoiServerTimestamp,
        lastFullSyncClientTimestamp = Clock.System.now().toEpochMilliseconds(),
        syncedDataVisibility = dataVisibility?.toProto()?.ordinal,
      )
    )
  }
}
