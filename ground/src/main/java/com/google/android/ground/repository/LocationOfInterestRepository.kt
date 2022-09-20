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
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.persistence.remote.NotFoundException
import com.google.android.ground.persistence.remote.RemoteDataEvent
import com.google.android.ground.persistence.remote.RemoteDataEvent.EventType.*
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.sync.DataSyncWorkManager
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import io.reactivex.*
import java.util.*
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
  private val localDataStore: LocalDataStore,
  private val localValueStore: LocalValueStore,
  private val remoteDataStore: RemoteDataStore,
  private val surveyRepository: SurveyRepository,
  private val dataSyncWorkManager: DataSyncWorkManager,
  private val authManager: AuthenticationManager,
  private val uuidGenerator: OfflineUuidGenerator
) {
  /**
   * Mirrors locations of interest in the specified survey from the remote db into the local db when
   * the network is available. When invoked, will first attempt to resync all locations of interest
   * from the remote db, subsequently syncing only remote changes. The returned stream never
   * completes, and subscriptions will only terminate on disposal.
   */
  fun syncLocationsOfInterest(survey: Survey): @Cold Completable {
    return remoteDataStore.loadLocationsOfInterestOnceAndStreamChanges(survey).flatMapCompletable {
      updateLocalLocationOfInterest(it)
    }
  }

  // TODO: Remove "location of interest" qualifier from this and other repository method names.
  private fun updateLocalLocationOfInterest(
    event: RemoteDataEvent<LocationOfInterest>
  ): @Cold Completable {
    return event.result.fold(
      { (entityId: String, entity: LocationOfInterest?) ->
        when (event.eventType) {
          ENTITY_LOADED,
          ENTITY_MODIFIED -> localDataStore.mergeLocationOfInterest(checkNotNull(entity))
          ENTITY_REMOVED -> localDataStore.deleteLocationOfInterest(entityId)
          else -> throw IllegalArgumentException()
        }
      },
      {
        Timber.d(it, "Invalid locations of interest in remote db ignored")
        Completable.complete()
      }
    )
  }

  // TODO: Only return location of interest fields needed to render locations of interest on map.
  fun getLocationsOfInterestOnceAndStream(
    survey: Survey
  ): @Cold(terminates = false) Flowable<ImmutableSet<LocationOfInterest>> =
    localDataStore.getLocationsOfInterestOnceAndStream(survey)

  fun getLocationOfInterest(
    locationOfInterestMutation: LocationOfInterestMutation
  ): @Cold Single<LocationOfInterest> =
    this.getLocationOfInterest(
      locationOfInterestMutation.surveyId,
      locationOfInterestMutation.locationOfInterestId
    )

  /** This only works if the survey and location of interests are already cached to local db. */
  fun getLocationOfInterest(
    surveyId: String,
    locationOfInterest: String
  ): @Cold Single<LocationOfInterest> =
    surveyRepository
      .getSurvey(surveyId)
      .flatMapMaybe { survey: Survey ->
        localDataStore.getLocationOfInterest(survey, locationOfInterest)
      }
      .switchIfEmpty(
        Single.error { NotFoundException("Location of interest not found $locationOfInterest") }
      )

  fun newMutation(
    surveyId: String,
    jobId: String,
    point: Point,
    date: Date
  ): LocationOfInterestMutation =
    LocationOfInterestMutation(
      jobId = jobId,
      geometry = point,
      type = Mutation.Type.CREATE,
      syncStatus = SyncStatus.PENDING,
      locationOfInterestId = uuidGenerator.generateUuid(),
      surveyId = surveyId,
      userId = authManager.currentUser.id,
      clientTimestamp = date
    )

  fun newPolygonOfInterestMutation(
    surveyId: String,
    jobId: String,
    vertices: ImmutableList<Point>,
    date: Date
  ): LocationOfInterestMutation =
    LocationOfInterestMutation(
      jobId = jobId,
      geometry = Polygon(LinearRing(vertices.map { it.coordinate })),
      type = Mutation.Type.CREATE,
      syncStatus = SyncStatus.PENDING,
      locationOfInterestId = uuidGenerator.generateUuid(),
      surveyId = surveyId,
      userId = authManager.currentUser.id,
      clientTimestamp = date
    )

  /**
   * Creates a mutation entry for the given parameters, applies it to the local db and schedules a
   * task for remote sync if the local transaction is successful.
   *
   * @param mutation Input [LocationOfInterestMutation]
   * @return If successful, returns the provided locations of interest wrapped as [Loadable]
   */
  fun applyAndEnqueue(mutation: LocationOfInterestMutation): @Cold Completable {
    val localTransaction = localDataStore.applyAndEnqueue(mutation)
    val remoteSync = dataSyncWorkManager.enqueueSyncWorker(mutation.locationOfInterestId)
    return localTransaction.andThen(remoteSync)
  }

  /**
   * Emits the list of [LocationOfInterestMutation] instances for a given location of interest which
   * have not yet been marked as [SyncStatus.COMPLETED], including pending, in progress, and failed
   * mutations. A new list is emitted on each subsequent change.
   */
  fun getIncompleteLocationOfInterestMutationsOnceAndStream(
    locationOfInterestId: String
  ): Flowable<ImmutableList<LocationOfInterestMutation>>? =
    localDataStore.getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
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
