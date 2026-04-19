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
package org.groundplatform.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.geometry.Geometry
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.model.mutation.LocationOfInterestMutation
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface

class FakeLocationOfInterestRepository : LocationOfInterestRepositoryInterface {
  var offlineLoi = FakeDataGenerator.newLocationOfInterest()
  var hasValidLois = true

  val syncLocationsOfInterestCall = FakeCall<Survey, Unit> {}

  override suspend fun syncLocationsOfInterest(survey: Survey) {
    syncLocationsOfInterestCall(survey)
  }

  override suspend fun getOfflineLoi(surveyId: String, loiId: String): LocationOfInterest =
    offlineLoi

  override suspend fun saveLoi(
    geometry: Geometry,
    job: Job,
    surveyId: String,
    loiName: String?,
    collectionId: String,
  ): String = offlineLoi.id

  override suspend fun applyAndEnqueue(mutation: LocationOfInterestMutation) {
    /* Nothing to do */
  }

  override suspend fun hasValidLois(surveyId: String): Boolean = hasValidLois

  override fun getValidLois(survey: Survey): Flow<Set<LocationOfInterest>> =
    flowOf(setOf(offlineLoi))

  override fun getWithinBounds(survey: Survey, bounds: Bounds): Flow<List<LocationOfInterest>> =
    flowOf(listOf(offlineLoi))

  override suspend fun deleteLoi(loi: LocationOfInterest) {
    /* Nothing to do */
  }
}
