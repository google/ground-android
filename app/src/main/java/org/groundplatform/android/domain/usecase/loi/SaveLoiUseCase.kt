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
package org.groundplatform.android.domain.usecase.loi

import javax.inject.Inject
import org.groundplatform.android.domain.repository.LocationOfInterestRepository
import org.groundplatform.android.model.geometry.Geometry
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.locationofinterest.generateProperties
import org.groundplatform.android.model.mutation.LocationOfInterestMutation
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.persistence.sync.MutationSyncWorkManager
import org.groundplatform.android.persistence.uuid.OfflineUuidGenerator
import org.groundplatform.android.repository.UserRepository

class SaveLoiUseCase
@Inject
constructor(
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val mutationSyncWorkManager: MutationSyncWorkManager,
  private val userRepository: UserRepository,
  private val uuidGenerator: OfflineUuidGenerator,
) {

  /**
   * Creates and saves a new [LocationOfInterest] locally and schedules it for remote
   * synchronization.
   *
   * @param collectionId The ID of the collection this LOI belongs to.
   * @param geometry The geometry of the new LOI.
   * @param job The job associated with the new LOI.
   * @param loiName An optional name for the LOI.
   * @param surveyId The ID of the survey this LOI belongs to.
   * @return The unique ID of the newly created [LocationOfInterest].
   */
  suspend operator fun invoke(
    collectionId: String,
    geometry: Geometry,
    job: Job,
    loiName: String?,
    surveyId: String,
  ): String {
    val newId = uuidGenerator.generateUuid()
    val mutation =
      LocationOfInterestMutation(
        jobId = job.id,
        type = Mutation.Type.CREATE,
        syncStatus = Mutation.SyncStatus.PENDING,
        surveyId = surveyId,
        locationOfInterestId = newId,
        userId = userRepository.getAuthenticatedUser().id,
        geometry = geometry,
        properties = generateProperties(loiName),
        isPredefined = false,
        collectionId = collectionId,
      )
    locationOfInterestRepository.applyAndEnqueue(mutation)
    mutationSyncWorkManager.enqueueSyncWorker()
    return newId
  }
}
