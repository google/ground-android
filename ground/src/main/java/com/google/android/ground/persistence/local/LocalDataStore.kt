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
package com.google.android.ground.persistence.local

import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.persistence.local.stores.*
import com.google.android.ground.rx.annotations.Cold
import com.google.common.collect.ImmutableList
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

/**
 * Provides access to local persistent data store, the canonical store for latest state and
 * mutations queued for sync with remote. Local changes are saved here before being written to the
 * remote data store, and both local and remote updates are always persisted here before being made
 * visible to the user.
 *
 * Implementations are expected to execute each method in this class as a single atomic transaction,
 * and must ensure all subscriptions are run in a background thread (i.e., not the Android main
 * thread).
 *
 * Note that long-lived streams return the full set of entities on each emission rather than deltas
 * to allow changes to not rely on prior UI state (i.e., emissions are idempotent).
 */
interface LocalDataStore {
  /** Provides access to [Survey] data in local storage. */
  var surveyStore: SurveyStore
  /** Provides access to [User] data in local storage. */
  var userStore: UserStore
  /** Provides access to [TileSet] data in local storage. */
  var tileSetStore: TileSetStore
  /** Provides access to [OfflineArea] data in local storage. */
  var offlineAreaStore: OfflineAreaStore
  /** Provides access to [Submission] data in local storage. */
  var submissionStore: SubmissionStore
  /** Provides access to [LocationOfInterest] data in local storage. */
  var locationOfInterestStore: LocationOfInterestStore

  /**
   * Returns a long-lived stream that emits the full list of mutations for specified survey on
   * subscribe and a new list on each subsequent change.
   */
  fun getMutationsOnceAndStream(
    survey: Survey
  ): @Cold(terminates = false) Flowable<ImmutableList<Mutation>>

  /**
   * Returns all LOI and submission mutations in the local mutation queue relating to LOI with the
   * specified id.
   */
  fun getPendingMutations(locationOfInterestId: String): @Cold Single<ImmutableList<Mutation>>

  /** Updates the provided list of mutations. */
  fun updateMutations(mutations: ImmutableList<Mutation>): @Cold Completable

  /**
   * Mark pending mutations as complete. If the mutation is of type DELETE, also removes the
   * corresponding submission or LOI.
   */
  fun finalizePendingMutations(mutations: ImmutableList<Mutation>): @Cold Completable
}
