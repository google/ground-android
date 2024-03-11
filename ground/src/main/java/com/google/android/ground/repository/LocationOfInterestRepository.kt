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
import com.google.android.ground.model.User
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.locationofinterest.generateProperties
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.persistence.local.stores.LocalLocationOfInterestStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.remote.NotFoundException
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.sync.MutationSyncWorkManager
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.gms.GmsExt.contains
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
  private val mutationSyncWorkManager: MutationSyncWorkManager,
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
  suspend fun getOfflineLoi(surveyId: String, locationOfInterest: String): LocationOfInterest =
    localSurveyStore.getSurveyById(surveyId)?.let {
      localLoiStore.getLocationOfInterest(it, locationOfInterest)
    } ?: throw NotFoundException("Location of interest not found $locationOfInterest")

  fun createLocationOfInterest(
    geometry: Geometry,
    job: Job,
    surveyId: String,
    user: User,
    loiName: String?,
  ): LocationOfInterest {
    val auditInfo = AuditInfo(user)
    return LocationOfInterest(
      id = uuidGenerator.generateUuid(),
      surveyId = surveyId,
      geometry = geometry,
      job = job,
      created = auditInfo,
      lastModified = auditInfo,
      ownerEmail = user.email,
      properties = generateProperties(loiName),
      isPlanned = false
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

  /** Returns a flow of all [LocationOfInterest] associated with the given [Survey]. */
  fun getLocationsOfInterests(survey: Survey): Flow<Set<LocationOfInterest>> =
    localLoiStore.findLocationsOfInterest(survey)

  /** Returns a list of geometries associated with the given [Survey]. */
  suspend fun getAllGeometries(survey: Survey): List<Geometry> =
    getLocationsOfInterests(survey).first().map { it.geometry }

  /** Returns a flow of all [LocationOfInterest] within the map bounds (viewport). */
  fun getWithinBounds(survey: Survey, bounds: Bounds): Flow<List<LocationOfInterest>> =
    getLocationsOfInterests(survey)
      .map { lois -> lois.filter { bounds.contains(it.geometry) } }
      .distinctUntilChanged()
}
