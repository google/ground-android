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

import com.google.android.ground.model.imagery.OfflineArea
import kotlinx.coroutines.flow.Flow

interface LocalOfflineAreaStore {
  /**
   * Attempts to update an offline area in the local data store. If the area doesn't exist, inserts
   * the area into the local data store.
   */
  suspend fun insertOrUpdate(area: OfflineArea)

  /** Returns all queued, failed, and completed offline areas from the local data store. */
  fun offlineAreas(): Flow<List<OfflineArea>>

  /** Delete an offline area and any associated tiles that are no longer needed. */
  suspend fun deleteOfflineArea(offlineAreaId: String)

  /** Returns the offline area with the specified id. */
  suspend fun getOfflineAreaById(id: String): OfflineArea?
}
