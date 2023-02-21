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
import com.google.android.ground.rx.annotations.Cold
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

interface LocationOfInterestStore : MutationStore<LocationOfInterestMutation, LocationOfInterest> {
  /**
   * Returns a long-lived stream that emits the full set of LOIs for a survey on subscribe, and
   * continues to return the full set each time a LOI is added/changed/removed.
   */
  fun getLocationsOfInterestOnceAndStream(
    survey: Survey
  ): @Cold(terminates = false) Flowable<Set<LocationOfInterest>>

  /** Returns the LOI with the specified UUID from the local data store, if found. */
  fun getLocationOfInterest(
    survey: Survey,
    locationOfInterestId: String
  ): @Cold Maybe<LocationOfInterest>

  /** Deletes LOI from local database. */
  fun deleteLocationOfInterest(locationOfInterestId: String): @Cold Completable

  /**
   * Emits the list of [LocationOfInterestMutation] instances for a given LOI which match the
   * provided `allowedStates`. A new list is emitted on each subsequent change.
   */
  fun getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Flowable<List<LocationOfInterestMutation>>

  fun getAllMutationsAndStream(): Flowable<List<LocationOfInterestMutationEntity>>

  fun findByLocationOfInterestId(
    id: String,
    vararg states: MutationEntitySyncStatus
  ): Single<List<LocationOfInterestMutationEntity>>

  suspend fun insertOrUpdate(loi: LocationOfInterest)

  suspend fun deleteNotIn(surveyId: String, ids: List<String>)
}
