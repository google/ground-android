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

import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestMutationEntity
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import kotlinx.coroutines.flow.Flow

interface LocalLocationOfInterestStore :
  LocalMutationStore<LocationOfInterestMutation, LocationOfInterest> {
  /**
   * Returns a main-safe flow that emits the full set of LOIs for a survey on subscribe, and
   * continues to return the full set each time a LOI is added/changed/removed.
   */
  fun findLocationsOfInterest(survey: Survey): Flow<Set<LocationOfInterest>>

  /** Returns the LOI with the specified UUID from the local data store, if found. */
  suspend fun getLocationOfInterest(
    survey: Survey,
    locationOfInterestId: String,
  ): LocationOfInterest?

  /** Deletes LOI from local database. */
  suspend fun deleteLocationOfInterest(locationOfInterestId: String)

  /**
   * Returns a [Flow] that emits a [List] of all [LocationOfInterestMutation]s stored in the local
   * db related to a given [Survey]. A new [List] is emitted on each change to the underlying saved
   * data.
   */
  fun getAllSurveyMutations(survey: Survey): Flow<List<LocationOfInterestMutation>>

  fun getAllMutationsFlow(): Flow<List<LocationOfInterestMutation>>

  suspend fun findByLocationOfInterestId(
    id: String,
    vararg states: MutationEntitySyncStatus,
  ): List<LocationOfInterestMutationEntity>

  suspend fun insertOrUpdate(loi: LocationOfInterest)

  suspend fun deleteNotIn(surveyId: String, ids: List<String>)
}
