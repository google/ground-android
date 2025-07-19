/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.domain.repository

import kotlinx.coroutines.flow.Flow
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.mutation.LocationOfInterestMutation
import org.groundplatform.android.ui.map.Bounds

/**
 * Defines operations for accessing and managing [LocationOfInterest] data.
 *
 * This includes synchronization with remote stores, local persistence, and querying.
 */
interface LocationOfInterestRepository {

  /**
   * Returns a [Flow] emitting the set of all valid (non-deleted) [LocationOfInterest]s for the
   * given [survey].
   *
   * @param survey The survey for which to retrieve LOIs.
   * @return A [Flow] emitting a [Set] of [LocationOfInterest]s.
   */
  fun getValidLois(survey: Survey): Flow<Set<LocationOfInterest>>

  /**
   * Returns a [Flow] emitting a list of [LocationOfInterest]s within the specified geographical
   * [bounds] for the given [survey].
   *
   * @param survey The survey to filter LOIs from.
   * @param bounds The geographical bounds to filter LOIs by.
   * @return A [Flow] emitting a [List] of [LocationOfInterest]s within the bounds.
   */
  fun getWithinBounds(survey: Survey, bounds: Bounds): Flow<List<LocationOfInterest>>

  /**
   * Synchronizes locations of interest for the specified [survey] between remote and local data
   * stores.
   *
   * @param survey The survey whose locations of interest are to be synchronized.
   */
  suspend fun syncLocationsOfInterest(survey: Survey)

  /**
   * Retrieves a [LocationOfInterest] from the local data store.
   *
   * Returns `null` if the survey or LOI is not found locally.
   *
   * @param surveyId The ID of the survey containing the LOI.
   * @param loiId The ID of the LOI to retrieve.
   * @return The [LocationOfInterest] if found, otherwise `null`.
   */
  suspend fun getOfflineLoi(surveyId: String, loiId: String): LocationOfInterest?

  /**
   * Applies a [LocationOfInterestMutation] to the local data store and enqueues it for
   * synchronization.
   *
   * @param mutation The mutation to apply and enqueue.
   */
  suspend fun applyAndEnqueue(mutation: LocationOfInterestMutation)

  /**
   * Checks if the specified [surveyId] has any valid (non-deleted) [LocationOfInterest]s in the
   * local storage.
   *
   * @param surveyId The ID of the survey to check.
   * @return `true` if valid LOIs exist for the survey, `false` otherwise.
   */
  suspend fun hasValidLois(surveyId: String): Boolean
}
