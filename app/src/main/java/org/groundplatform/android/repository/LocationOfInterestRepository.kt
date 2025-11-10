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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.groundplatform.android.data.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.data.remote.RemoteDataStore
import org.groundplatform.android.data.sync.MutationSyncWorkManager
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.model.Role
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.geometry.Geometry
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.locationofinterest.generateProperties
import org.groundplatform.android.model.map.Bounds
import org.groundplatform.android.model.mutation.LocationOfInterestMutation
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.model.mutation.Mutation.SyncStatus
import org.groundplatform.android.proto.Survey.DataVisibility
import org.groundplatform.android.system.auth.AuthenticationManager
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
  suspend fun syncLocationsOfInterest(survey: Survey) = coroutineScope {
    val ownerUserId = authenticationManager.getAuthenticatedUser().id

    val predefinedDeferred = async { remoteDataStore.loadPredefinedLois(survey) }
    val userDeferred = async { remoteDataStore.loadUserLois(survey, ownerUserId) }
    val sharedDeferred = async {
      if (survey.dataVisibility == DataVisibility.ALL_SURVEY_PARTICIPANTS) {
        remoteDataStore.loadSharedLois(survey)
      } else {
        emptyList()
      }
    }

    val (predefinedLois, userLois, sharedLois) =
      awaitAll(predefinedDeferred, userDeferred, sharedDeferred)

    val allLois = predefinedLois + userLois + sharedLois

    val mutations = localLoiStore.getAllSurveyMutations(survey).firstOrNull().orEmpty()

    // NOTE(#2652): Don't delete pending locations of interest, since we can accidentally delete
    // them here if we get to this routine before they can be synced up to the remote database.
    val pendingLois =
      mutations
        .asSequence()
        .filter { it.syncStatus in setOf(SyncStatus.PENDING, SyncStatus.IN_PROGRESS) }
        .map { it.locationOfInterestId }
        .toList()

    mergeAll(survey.id, allLois, pendingLois)
  }

  private suspend fun mergeAll(
    surveyId: String,
    lois: List<LocationOfInterest>,
    pendingLois: List<String>,
  ) {
    // Insert new or update existing LOIs in local db.
    lois.forEach { validateAndInsertOrUpdate(it) }
    // Delete LOIs in local db not returned in latest list from server, skipping pending mutations.
    localLoiStore.deleteNotIn(surveyId, lois.map { it.id } + pendingLois)
  }

  /**
   * Validates LOI geometry before inserting or updating it in the local store. Throws
   * IllegalArgumentException if the geometry has empty coordinates.
   */
  private suspend fun validateAndInsertOrUpdate(loi: LocationOfInterest) {
    require(!loi.geometry.isEmpty()) {
      "Attempted to save LOI ${loi.id} with empty geometry. LOI: $loi"
    }

    localLoiStore.insertOrUpdate(loi)
  }

  /** This only works if the survey and location of interests are already cached to local db. */
  suspend fun getOfflineLoi(surveyId: String, loiId: String): LocationOfInterest? {
    val survey = localSurveyStore.getSurveyById(surveyId)
    val locationOfInterest = survey?.let { localLoiStore.getLocationOfInterest(it, loiId) }

    if (survey == null) {
      Timber.e("Survey not found: $surveyId")
    } else if (locationOfInterest == null) {
      Timber.e("LOI not found for survey $surveyId: LOI ID $loiId")
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
    when (mutation.type) {
      Mutation.Type.CREATE -> {
        val geometry =
          requireNotNull(mutation.geometry) {
            "CREATE mutation requires geometry. Mutation: $mutation"
          }
        require(!geometry.isEmpty()) {
          "Attempted to apply CREATE with empty ${geometry::class.simpleName} geometry. Mutation: $mutation"
        }
      }

      Mutation.Type.UPDATE -> {
        // Partial updates may omit geometry. If present, it must be non-empty.
        mutation.geometry?.let { g ->
          require(!g.isEmpty()) {
            "Attempted to apply UPDATE with empty ${g::class.simpleName} geometry. Mutation: $mutation"
          }
        }
      }

      else -> {
        // DELETE / others — no geometry validation needed
      }
    }

    localLoiStore.applyAndEnqueue(mutation)
    mutationSyncWorkManager.enqueueSyncWorker()
  }

  /**
   * Returns true if [Survey] for the given [surveyId] has at least one valid [LocationOfInterest]
   * in the local storage.
   */
  suspend fun hasValidLois(surveyId: String): Boolean = localLoiStore.getLoiCount(surveyId) > 0

  /** Returns a flow of all valid (not deleted) [LocationOfInterest] in the given [Survey]. */
  fun getValidLois(survey: Survey): Flow<Set<LocationOfInterest>> =
    localLoiStore.getValidLois(survey).map { lois ->
      // Filter out LOIs with invalid/empty geometries to prevent crashes
      lois
        .filter { loi ->
          val isValid = !loi.geometry.isEmpty()
          if (!isValid) {
            Timber.w("Filtering out LOI ${loi.id} with empty coordinates: $loi")
          }
          isValid
        }
        .toSet()
    }

  /** Returns a flow of all [LocationOfInterest] within the map bounds (viewport). */
  fun getWithinBounds(survey: Survey, bounds: Bounds): Flow<List<LocationOfInterest>> =
    getValidLois(survey)
      .map { lois -> lois.filter { bounds.contains(it.geometry) } }
      .distinctUntilChanged()

  /**
   * Deletes a LOI by creating a DELETE mutation, applying it to the local db, and scheduling a task
   * for remote sync. In free-form jobs, this will also delete associated submissions.
   *
   * @param loi The LocationOfInterest to delete
   * @throws IllegalStateException if the LOI is predefined or the user doesn't have permission to
   *   delete it
   */
  suspend fun deleteLoi(loi: LocationOfInterest) {
    if (loi.isPredefined == true) {
      error("Cannot delete predefined LOI: ${loi.id}")
    }

    val user = userRepository.getAuthenticatedUser()
    val ownerId = loi.created.user.id
    val isOwner = ownerId == user.id

    val survey = localSurveyStore.getSurveyById(loi.surveyId)
    val isOrganizer =
      survey?.let {
        runCatching { it.getRole(user.email) }
          .getOrNull()
          ?.let { role -> role == Role.SURVEY_ORGANIZER } ?: false
      } ?: false

    if (!isOwner && !isOrganizer) {
      error(
        "User ${user.id} does not have permission to delete LOI ${loi.id}. " +
          "User must be the owner or a survey organizer."
      )
    }

    val mutation = loi.toMutation(Mutation.Type.DELETE, user.id)
    applyAndEnqueue(mutation)
  }
}
