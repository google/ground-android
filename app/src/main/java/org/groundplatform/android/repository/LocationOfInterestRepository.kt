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
package org.groundplatform.android.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.geometry.Geometry
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.locationofinterest.generateProperties
import org.groundplatform.android.model.mutation.LocationOfInterestMutation
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.model.mutation.Mutation.SyncStatus
import org.groundplatform.android.persistence.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.groundplatform.android.persistence.remote.RemoteDataStore
import org.groundplatform.android.persistence.sync.MutationSyncWorkManager
import org.groundplatform.android.persistence.uuid.OfflineUuidGenerator
import org.groundplatform.android.system.auth.AuthenticationManager
import org.groundplatform.android.ui.map.Bounds
import org.groundplatform.android.ui.map.gms.GmsExt.contains
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
  private val mutationSyncWorkManager: MutationSyncWorkManager,
  private val userRepository: UserRepository,
  private val uuidGenerator: OfflineUuidGenerator,
  private val authenticationManager: AuthenticationManager,
) {
  /** Mirrors locations of interest in the specified survey from the remote db into the local db. */
  suspend fun syncLocationsOfInterest(survey: Survey) {
    // TODO: Allow survey organizers to make ad hoc LOIs visible to all data collectors.
    // Issue URL: https://github.com/google/ground-android/issues/2384
    val ownerUserId = authenticationManager.getAuthenticatedUser().id
    val lois =
      with(remoteDataStore) { loadPredefinedLois(survey) + loadUserLois(survey, ownerUserId) }
    val loiMutations = localLoiStore.getAllSurveyMutations(survey).firstOrNull() ?: listOf()
    // NOTE(#2652): Don't delete pending locations of interest, since we can accidentally delete
    // them here if we get to this routine before they can be synced up to the remote database.
    val pendingLois =
      loiMutations.mapNotNull {
        when (it.syncStatus) {
          SyncStatus.PENDING,
          SyncStatus.IN_PROGRESS -> it.locationOfInterestId
          else -> null
        }
      }
    mergeAll(survey.id, lois, pendingLois)
  }

  private suspend fun mergeAll(
    surveyId: String,
    lois: List<LocationOfInterest>,
    pendingLois: List<String>,
  ) {
    // Insert new or update existing LOIs in local db.
    lois.forEach { localLoiStore.insertOrUpdate(it) }
    // Delete LOIs in local db not returned in latest list from server, skipping pending mutations.
    localLoiStore.deleteNotIn(surveyId, lois.map { it.id } + pendingLois)
  }

  /** This only works if the survey and location of interests are already cached to local db. */
  suspend fun getOfflineLoi(surveyId: String, loiId: String): LocationOfInterest? {
    val survey = localSurveyStore.getSurveyById(surveyId)
    val locationOfInterest = survey?.let { localLoiStore.getLocationOfInterest(it, loiId) }

    if (survey == null) {
      Timber.e("LocationOfInterestRepository", "Survey not found: $surveyId")
    } else if (locationOfInterest == null) {
      Timber.e("LocationRepository", "LOI not found for survey $surveyId: LOI ID $loiId")
    }
    return locationOfInterest
  }

  /** Saves a new LOI in the local db and enqueues a sync worker. */
  suspend fun saveLoi(
    geometry: Geometry,
    job: Job,
    surveyId: String,
    loiName: String?,
    collectionId: String,
  ): String {
    val newId = uuidGenerator.generateUuid()
    val user = userRepository.getAuthenticatedUser()
    val mutation =
      LocationOfInterestMutation(
        jobId = job.id,
        type = Mutation.Type.CREATE,
        syncStatus = SyncStatus.PENDING,
        surveyId = surveyId,
        locationOfInterestId = newId,
        userId = user.id,
        geometry = geometry,
        properties = generateProperties(loiName),
        isPredefined = false,
        collectionId = collectionId,
      )
    applyAndEnqueue(mutation)
    return newId
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
    mutationSyncWorkManager.enqueueSyncWorker()
  }

  /** Returns a flow of all valid (not deleted) [LocationOfInterest] in the given [Survey]. */
  fun getValidLois(survey: Survey): Flow<Set<LocationOfInterest>> =
    localLoiStore.getValidLois(survey)

  /** Returns a flow of all [LocationOfInterest] within the map bounds (viewport). */
  fun getWithinBounds(survey: Survey, bounds: Bounds): Flow<List<LocationOfInterest>> =
    getValidLois(survey)
      .map { lois -> lois.filter { bounds.contains(it.geometry) } }
      .distinctUntilChanged()
}
