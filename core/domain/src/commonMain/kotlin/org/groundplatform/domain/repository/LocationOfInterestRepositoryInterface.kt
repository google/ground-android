/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.domain.repository

import kotlinx.coroutines.flow.Flow
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.geometry.Geometry
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.model.mutation.LocationOfInterestMutation

interface LocationOfInterestRepositoryInterface {
  /** Mirrors locations of interest in the specified survey from the remote db into the local db. */
  suspend fun syncLocationsOfInterest(survey: Survey)

  /** This only works if the survey and location of interests are already cached to local db. */
  suspend fun getOfflineLoi(surveyId: String, loiId: String): LocationOfInterest?
  fun getLoiFlow(surveyId: String, loiId: String): Flow<LocationOfInterest?>

  /** Saves a new LOI in the local db and enqueues a sync worker. */
  suspend fun saveLoi(
    geometry: Geometry,
    job: Job,
    surveyId: String,
    loiName: String?,
    collectionId: String,
  ): String

  /**
   * Creates a mutation entry for the given parameters, applies it to the local db and schedules a
   * task for remote sync if the local transaction is successful.
   *
   * @param mutation Input [LocationOfInterestMutation]
   * @return If successful, returns the provided locations of interest wrapped as `Loadable`
   */
  suspend fun applyAndEnqueue(mutation: LocationOfInterestMutation)

  /**
   * Returns true if [Survey] for the given [surveyId] has at least one valid [LocationOfInterest]
   * in the local storage.
   */
  suspend fun hasValidLois(surveyId: String): Boolean

  /** Returns a flow of all valid (not deleted) [LocationOfInterest] in the given [Survey]. */
  fun getValidLois(survey: Survey): Flow<Set<LocationOfInterest>>

  /** Returns a flow of all [LocationOfInterest] within the map bounds (viewport). */
  fun getWithinBounds(survey: Survey, bounds: Bounds): Flow<List<LocationOfInterest>>

  /**
   * Deletes a LOI by creating a DELETE mutation, applying it to the local db, and scheduling a task
   * for remote sync. In free-form jobs, this will also delete associated submissions.
   *
   * @param loi The LocationOfInterest to delete
   * @throws IllegalStateException if the LOI is predefined or the user doesn't have permission to
   *   delete it
   */
  suspend fun deleteLoi(loi: LocationOfInterest)
}
