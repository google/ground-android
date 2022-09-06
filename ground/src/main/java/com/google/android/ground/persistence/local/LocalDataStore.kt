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
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.persistence.local.room.LocalDataStoreException
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.rx.annotations.Cold
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
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
  /** Load surveys stored in local database. */
  val surveys: @Cold Single<ImmutableList<Survey>>

  /** Load last active survey, if any. */
  fun getSurveyById(id: String): @Cold Maybe<Survey>

  /** Delete stored survey from database. */
  fun deleteSurvey(survey: Survey): @Cold Completable

  /** Add survey to the database. */
  fun insertOrUpdateSurvey(survey: Survey): @Cold Completable

  /** Add user to the database. */
  fun insertOrUpdateUser(user: User): @Cold Completable

  /**
   * Loads the user with the specified id from the local data store. The returned Single fails with
   * [java.util.NoSuchElementException] if not found.
   */
  fun getUser(id: String): @Cold Single<User>

  /**
   * Applies the specified [LocationOfInterestMutation] to the local data store, appending the
   * mutation to the local queue for remote sync.
   */
  fun applyAndEnqueue(mutation: LocationOfInterestMutation): @Cold Completable

  /**
   * Applies the specified [SubmissionMutation] to the local data store, appending the mutation to
   * the local queue for remote sync.
   */
  fun applyAndEnqueue(mutation: SubmissionMutation): @Cold Completable

  /** Applies the specified [SubmissionMutation] to the local data store.. */
  @Throws(LocalDataStoreException::class) fun apply(mutation: SubmissionMutation): @Cold Completable

  /**
   * Returns a long-lived stream that emits the full set of LOIs for a survey on subscribe, and
   * continues to return the full set each time a LOI is added/changed/removed.
   */
  fun getLocationsOfInterestOnceAndStream(
    survey: Survey
  ): @Cold(terminates = false) Flowable<ImmutableSet<LocationOfInterest>>

  /**
   * Returns the list of submissions which are not marked for deletion for the specified
   * locationOfInterest and job.
   */
  fun getSubmissions(
    locationOfInterest: LocationOfInterest,
    jobId: String
  ): @Cold Single<ImmutableList<Submission>>

  /** Returns the LOI with the specified UUID from the local data store, if found. */
  fun getLocationOfInterest(
    survey: Survey,
    locationOfInterestId: String
  ): @Cold Maybe<LocationOfInterest>

  /** Returns the submission with the specified UUID from the local data store, if found. */
  fun getSubmission(
    locationOfInterest: LocationOfInterest,
    submissionId: String
  ): @Cold Maybe<Submission>

  /**
   * Returns a long-lived stream that emits the full set of tiles on subscribe and continues to
   * return the full set each time a tile is added/changed/removed.
   */
  val tileSetsOnceAndStream: @Cold(terminates = false) Flowable<ImmutableSet<TileSet>>

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

  /**
   * Merges the provided locationOfInterest with pending unsynced local mutations, and inserts it
   * into the local data store. If a locationOfInterest with the same id already exists, it will be
   * overwritten with the merged instance.
   */
  fun mergeLocationOfInterest(locationOfInterest: LocationOfInterest): @Cold Completable

  /** Deletes LOI from local database. */
  fun deleteLocationOfInterest(locationOfInterestId: String): @Cold Completable

  /**
   * Merges the provided submission with pending unsynced local mutations, and inserts it into the
   * local data store. If a submission with the same id already exists, it will be overwritten with
   * the merged instance.
   */
  fun mergeSubmission(submission: Submission): @Cold Completable

  /** Deletes submission from local database. */
  fun deleteSubmission(submissionId: String): @Cold Completable

  /**
   * Attempts to update a tile in the local data store. If the tile doesn't exist, inserts the tile
   * into the local data store.
   */
  fun insertOrUpdateTileSet(tileSet: TileSet): @Cold Completable

  /** Returns the tile with the specified URL from the local data store, if found. */
  fun getTileSet(tileUrl: String): @Cold Maybe<TileSet>

  /** Returns all pending tiles from the local data store. */
  val pendingTileSets: @Cold Single<ImmutableList<TileSet>>

  /**
   * Attempts to update an offline area in the local data store. If the area doesn't exist, inserts
   * the area into the local data store.
   */
  fun insertOrUpdateOfflineArea(area: OfflineArea): @Cold Completable

  /** Returns all queued, failed, and completed offline areas from the local data store. */
  val offlineAreasOnceAndStream: @Cold(terminates = false) Flowable<ImmutableList<OfflineArea>>

  /** Delete an offline area and any associated tiles that are no longer needed. */
  fun deleteOfflineArea(offlineAreaId: String): @Cold Completable

  /** Returns the offline area with the specified id. */
  fun getOfflineAreaById(id: String): Single<OfflineArea>

  /**
   * Update the area count of an existing tile source in the local data store with the area count of
   * [TileSet].
   */
  fun updateTileSetOfflineAreaReferenceCountByUrl(newCount: Int, url: String): @Cold Completable

  /** Delete a tile source associated with a given URL from the local data store. */
  fun deleteTileSetByUrl(tileSet: TileSet): @Cold Completable

  /**
   * Emits the list of [LocationOfInterestMutation] instances for a given LOI which match the
   * provided `allowedStates`. A new list is emitted on each subsequent change.
   */
  fun getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Flowable<ImmutableList<LocationOfInterestMutation>>

  /**
   * Emits the list of [SubmissionMutation] instances for a given LOI which match the provided
   * `allowedStates`. A new list is emitted on each subsequent change.
   */
  fun getSubmissionMutationsByLocationOfInterestIdOnceAndStream(
    survey: Survey,
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Flowable<ImmutableList<SubmissionMutation>>
}
