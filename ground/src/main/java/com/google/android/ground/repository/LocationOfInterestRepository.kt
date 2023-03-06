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

import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.local.stores.LocalLocationOfInterestStore
import com.google.android.ground.persistence.remote.NotFoundException
import com.google.android.ground.persistence.remote.RemoteDataEvent
import com.google.android.ground.persistence.remote.RemoteDataEvent.EventType.*
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.sync.MutationSyncWorkManager
import com.google.android.ground.rx.annotations.Cold
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Coordinates persistence and retrieval of [LocationOfInterest] instances from remote, local, and
 * in memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
class LocationOfInterestRepository
@Inject
constructor(
  private val localSurveyStore: LocalSurveyStore,
  private val localLoiStore: LocalLocationOfInterestStore,
  private val remoteDataStore: RemoteDataStore,
  private val mutationSyncWorkManager: MutationSyncWorkManager
) {
  /** Mirrors locations of interest in the specified survey from the remote db into the local db. */
  suspend fun syncAll(survey: Survey) {
    val lois = remoteDataStore.loadLocationsOfInterest(survey)
    mergeAll(survey.id, lois)
  }

  private suspend fun mergeAll(surveyId: String, lois: List<LocationOfInterest>) {
    // Insert new or update existing LOIs in local db.
    lois.forEach { localLoiStore.insertOrUpdate(it) }

    // Delete LOIs in local db not returned in latest list from server.
    localLoiStore.deleteNotIn(surveyId, lois.map { it.id })
  }

  // TODO: Remove "location of interest" qualifier from this and other repository method names.
  private fun updateLocalLocationOfInterest(
    event: RemoteDataEvent<LocationOfInterest>
  ): @Cold Completable =
    event.result.fold(
      { (entityId: String, entity: LocationOfInterest?) ->
        when (event.eventType) {
          ENTITY_LOADED,
          ENTITY_MODIFIED -> localLoiStore.merge(checkNotNull(entity))
          ENTITY_REMOVED -> localLoiStore.deleteLocationOfInterest(entityId)
          else -> throw IllegalArgumentException("Unknown eventType ${event.eventType}")
        }
      },
      {
        Timber.d(it, "Invalid locations of interest in remote db ignored")
        Completable.complete()
      }
    )

  /** This only works if the survey and location of interests are already cached to local db. */
  fun getOfflineLocationOfInterest(
    surveyId: String,
    locationOfInterest: String
  ): @Cold Single<LocationOfInterest> =
    localSurveyStore
      .getSurveyById(surveyId)
      .flatMap() { survey: Survey ->
        localLoiStore.getLocationOfInterest(survey, locationOfInterest)
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
    val localTransaction = localLoiStore.applyAndEnqueue(mutation)
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
  ): Flowable<List<LocationOfInterestMutation>> =
    localLoiStore.getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
      locationOfInterestId,
      MutationEntitySyncStatus.PENDING,
      MutationEntitySyncStatus.IN_PROGRESS,
      MutationEntitySyncStatus.FAILED
    )

  /** Returns a flowable of all [LocationOfInterest] for the currently active [Survey]. */
  fun getLocationsOfInterestOnceAndStream(survey: Survey): Flowable<Set<LocationOfInterest>> =
    localLoiStore.getLocationsOfInterestOnceAndStream(survey)

  /** Returns a flowable of all [LocationOfInterest] within the map bounds (viewport). */
  fun getWithinBoundsOnceAndStream(
    survey: Survey,
    cameraBoundUpdates: Flowable<LatLngBounds>
  ): Flowable<List<LocationOfInterest>> =
    cameraBoundUpdates
      .flatMap { bounds ->
        getLocationsOfInterestOnceAndStream(survey).map { lois ->
          lois.filter { it.geometry.isWithinBounds(bounds) }
        }
      }
      .distinctUntilChanged()
}
