/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.repository

import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.persistence.remote.NotFoundException
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.sync.MutationSyncWorkManager
import com.google.android.ground.rx.annotations.Cold
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates persistence and retrieval of [LocationOfInterest] instances from remote, local, and
 * in memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
class LocationOfInterestRepository
@Inject
constructor(
  private val localDataStore: LocalDataStore,
  private val localValueStore: LocalValueStore,
  private val remoteDataStore: RemoteDataStore,
  private val surveyRepository: SurveyRepository,
  private val mutationSyncWorkManager: MutationSyncWorkManager
) {
  private val locationOfInterestStore = this.localDataStore.localLocationOfInterestStore

  /** Mirrors locations of interest in the specified survey from the remote db into the local db. */
  fun syncAll(survey: Survey): @Cold Completable =
    remoteDataStore.loadLocationsOfInterest(survey).flatMapCompletable { mergeAll(survey.id, it) }

  private fun mergeAll(surveyId: String, lois: List<LocationOfInterest>): @Cold Completable {
    // Insert new or update existing LOIs in local db.
    val insertOrUpdate = lois.map { localDataStore.localLocationOfInterestStore.insertOrUpdate(it) }

    // Delete LOIs in local db not returned in latest list from server.
    val deleteNotIn =
      localDataStore.localLocationOfInterestStore.deleteNotIn(surveyId, lois.map { it.id })

    return Completable.concat(insertOrUpdate).andThen(deleteNotIn)
  }

  // TODO: Only return location of interest fields needed to render locations of interest on map.
  fun getLocationsOfInterestOnceAndStream(
    survey: Survey
  ): @Cold(terminates = false) Flowable<ImmutableSet<LocationOfInterest>> =
    locationOfInterestStore.getLocationsOfInterestOnceAndStream(survey)

  /** This only works if the survey and location of interests are already cached to local db. */
  fun getLocationOfInterest(
    surveyId: String,
    locationOfInterest: String
  ): @Cold Single<LocationOfInterest> =
    surveyRepository
      .getSurvey(surveyId)
      .flatMapMaybe { survey: Survey ->
        locationOfInterestStore.getLocationOfInterest(survey, locationOfInterest)
      }
      .switchIfEmpty(
        Single.error { NotFoundException("Location of interest not found $locationOfInterest") }
      )

  /**
   * Creates a mutation entry for the given parameters, applies it to the local db and schedules a
   * task for remote sync if the local transaction is successful.
   *
   * @param mutation Input [LocationOfInterestMutation]
   * @return If successful, returns the provided locations of interest wrapped as `Loadable`
   */
  fun applyAndEnqueue(mutation: LocationOfInterestMutation): @Cold Completable {
    val localTransaction = localDataStore.localLocationOfInterestStore.applyAndEnqueue(mutation)
    val remoteSync = mutationSyncWorkManager.enqueueSyncWorker(mutation.locationOfInterestId)
    return localTransaction.andThen(remoteSync)
  }

  /**
   * Emits the list of [LocationOfInterestMutation] instances for a given location of interest which
   * have not yet been marked as [SyncStatus.COMPLETED], including pending, in progress, and failed
   * mutations. A new list is emitted on each subsequent change.
   */
  fun getIncompleteLocationOfInterestMutationsOnceAndStream(
    locationOfInterestId: String
  ): Flowable<ImmutableList<LocationOfInterestMutation>> =
    locationOfInterestStore.getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
      locationOfInterestId,
      MutationEntitySyncStatus.PENDING,
      MutationEntitySyncStatus.IN_PROGRESS,
      MutationEntitySyncStatus.FAILED
    )

  val isPolygonInfoDialogShown: Boolean
    get() = localValueStore.isPolygonInfoDialogShown

  fun setPolygonDialogInfoShown(value: Boolean) {
    localValueStore.isPolygonInfoDialogShown = value
  }
}
