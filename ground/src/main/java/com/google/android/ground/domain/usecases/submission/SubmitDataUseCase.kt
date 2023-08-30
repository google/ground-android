/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.domain.usecases.submission

import androidx.room.Transaction
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.submission.GeometryData
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.system.auth.AuthenticationManager
import javax.inject.Inject
import timber.log.Timber

class SubmitDataUseCase
@Inject
constructor(
  private val authManager: AuthenticationManager,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val submissionRepository: SubmissionRepository,
) {

  /**
   * Creates a Submission for the given [job] with the user's responses from the [taskDataDeltas].
   * If [loiId] is null a new LOI is created based on the first [TaskDataDelta] since the Suggest
   * LOI task is the first task in the Data Collection flow when a new LOI is being suggested.
   */
  @Transaction
  @Suppress("UseIfInsteadOfWhen")
  suspend operator fun invoke(
    loiId: String?,
    job: Job,
    surveyId: String,
    taskDataDeltas: List<TaskDataDelta>
  ) {
    Timber.v("Submitting data for loi: $loiId")
    var loiIdToSubmit = loiId
    val taskDataDeltasToSubmit = taskDataDeltas.toMutableList()

    if (loiId == null) {
      // loiIds are null for Suggest LOI data collection flows
      when (val suggestLoiTaskData = taskDataDeltasToSubmit.removeAt(0).newTaskData) {
        is GeometryData -> loiIdToSubmit = saveLoi(suggestLoiTaskData.geometry, job, surveyId).id
        else -> error("No suggest LOI Task found when loi ID was null")
      }
    }

    submissionRepository
      .saveSubmission(
        surveyId,
        requireNotNull(loiIdToSubmit) { "No LOI found present for submission" },
        taskDataDeltasToSubmit
      )
      .blockingAwait()
  }

  private suspend fun saveLoi(geometry: Geometry, job: Job, surveyId: String): LocationOfInterest {
    val loi = locationOfInterestRepository.createLocationOfInterest(geometry, job, surveyId)
    locationOfInterestRepository.applyAndEnqueue(
      loi.toMutation(Mutation.Type.CREATE, authManager.currentUser.id)
    )
    return loi
  }
}
