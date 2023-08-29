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

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.stores.LocalLocationOfInterestStore
import com.google.android.ground.persistence.local.stores.LocalSubmissionStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.remote.NotFoundException
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.sync.MutationSyncWorkManager
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.FeatureType
import com.google.android.ground.ui.map.gms.GmsExt.contains
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.rx2.asFlowable

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
  private val localSubmissionStore: LocalSubmissionStore,
  private val remoteDataStore: RemoteDataStore,
  private val mutationSyncWorkManager: MutationSyncWorkManager,
  private val authManager: AuthenticationManager,
  private val uuidGenerator: OfflineUuidGenerator
) {
  /** Mirrors locations of interest in the specified survey from the remote db into the local db. */
  suspend fun syncLocationsOfInterest(survey: Survey) {
    val lois = remoteDataStore.loadLocationsOfInterest(survey)
    mergeAll(survey.id, lois)
  }

  private suspend fun mergeAll(surveyId: String, lois: List<LocationOfInterest>) {
    // Insert new or update existing LOIs in local db.
    lois.forEach { localLoiStore.insertOrUpdate(it) }

    // Delete LOIs in local db not returned in latest list from server.
    localLoiStore.deleteNotIn(surveyId, lois.map { it.id })
  }

  /** This only works if the survey and location of interests are already cached to local db. */
  fun getOfflineLocationOfInterest(
    surveyId: String,
    locationOfInterest: String
  ): @Cold Single<LocationOfInterest> =
    localSurveyStore
      .getSurveyById(surveyId)
      .flatMap { survey: Survey -> localLoiStore.getLocationOfInterest(survey, locationOfInterest) }
      .switchIfEmpty(
        Single.error { NotFoundException("Location of interest not found $locationOfInterest") }
      )

  fun createLocationOfInterest(geometry: Geometry, job: Job, surveyId: String): LocationOfInterest {
    val auditInfo = AuditInfo(authManager.currentUser)
    return LocationOfInterest(
      id = uuidGenerator.generateUuid(),
      surveyId = surveyId,
      geometry = geometry,
      job = job,
      created = auditInfo,
      lastModified = auditInfo
    )
  }

  /**
   * Creates a mutation entry for the given parameters, applies it to the local db and schedules a
   * task for remote sync if the local transaction is successful.
   *
   * @param mutation Input [LocationOfInterestMutation]
   * @return If successful, returns the provided locations of interest wrapped as `Loadable`
   */
  suspend fun applyAndEnqueue(mutation: LocationOfInterestMutation) {
    localLoiStore.applyAndEnqueue(mutation)
    mutationSyncWorkManager.enqueueSyncWorker(mutation.locationOfInterestId)
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

  /** Returns a flowable of all [LocationOfInterest] for the given [Survey]. */
  private fun getLocationsOfInterestOnceAndStream(survey: Survey): Flowable<Set<LocationOfInterest>> =
    localLoiStore.getLocationsOfInterestOnceAndStream(survey)

  private fun findLocationsOfInterest(survey: Survey) =
    localLoiStore.findLocationsOfInterest(survey)

  fun findLocationsOfInterestFeatures(survey: Survey) =
    findLocationsOfInterest(survey)
      .map { toLocationOfInterestFeatures(it) }

  private suspend fun toLocationOfInterestFeatures(
    locationsOfInterest: Set<LocationOfInterest>
  ): Set<Feature> = // TODO: Add support for polylines similar to mapPins.
    locationsOfInterest
      .map {
        val pendingSubmissions =
          localSubmissionStore.getPendingSubmissionCountByLocationOfInterestId(it.id)
        val submissionCount = it.submissionCount + pendingSubmissions
        Feature(
          id = it.id,
          type = FeatureType.LOCATION_OF_INTEREST.ordinal,
          flag = submissionCount > 0,
          geometry = it.geometry
        )
      }
      .toPersistentSet()

  /** Returns a list of geometries associated with the given [Survey]. */
  suspend fun getAllGeometries(survey: Survey): List<Geometry> =
    getLocationsOfInterestOnceAndStream(survey).awaitFirst().map { it.geometry }

  /** Returns a flowable of all [LocationOfInterest] within the map bounds (viewport). */
  fun getWithinBoundsOnceAndStream(
    survey: Survey,
    cameraBoundUpdates: Flowable<Bounds>
  ): Flowable<List<LocationOfInterest>> =
    cameraBoundUpdates
      .switchMap { bounds ->
        getLocationsOfInterestOnceAndStream(survey).map { lois ->
          lois.filter { bounds.contains(it.geometry) }
        }
      }
      .distinctUntilChanged()
}
