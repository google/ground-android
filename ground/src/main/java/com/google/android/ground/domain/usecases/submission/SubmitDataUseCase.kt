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
import com.google.android.ground.model.submission.GeometryTaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.UserRepository
import javax.inject.Inject
import timber.log.Timber

class SubmitDataUseCase
@Inject
constructor(
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val submissionRepository: SubmissionRepository,
  private val userRepository: UserRepository
) {

  /**
   * Creates a Submission for the given [job] with collected data defines as a collection of
   * [ValueDelta]s. If [selectedLoiId] is null, a new LOI is created based on the first [ValueDelta]
   * since the Suggest LOI task is the first task in the Data Collection flow when a new LOI is
   * being suggested. The supplied [loiName] will then be stored as the "name" in the properties.
   */
  @Transaction
  suspend operator fun invoke(
    selectedLoiId: String?,
    job: Job,
    surveyId: String,
    deltas: List<ValueDelta>,
    loiName: String?,
  ) {
    Timber.v("Submitting data for LOI: $selectedLoiId")
    val deltasToSubmit = deltas.toMutableList()
    val submissionLoiId =
      selectedLoiId ?: addLocationOfInterest(surveyId, job, deltasToSubmit, loiName)
    submissionRepository.saveSubmission(surveyId, submissionLoiId, deltasToSubmit)
  }

  /**
   * Extracts and removes the response to the "add LOI" task from the provided deltas and stores the
   * new LOI to the local mutation queue and db.
   */
  private suspend fun addLocationOfInterest(
    surveyId: String,
    job: Job,
    deltas: MutableList<ValueDelta>,
    loiName: String?,
  ): String {
    val addLoiTask = job.getAddLoiTask() ?: error("Null LOI ID but no add LOI task")
    val addLoiTaskId = deltas.indexOfFirst { it.taskId == addLoiTask.id }
    if (addLoiTaskId < 0) error("Add LOI task response missing")
    val addLoiValue = deltas.removeAt(addLoiTaskId).newTaskData
    if (addLoiValue !is GeometryTaskData) error("Invalid add LOI task response")
    return saveLoi(addLoiValue.geometry, job, surveyId, loiName).id
  }

  private suspend fun saveLoi(
    geometry: Geometry,
    job: Job,
    surveyId: String,
    loiName: String?,
  ): LocationOfInterest {
    val user = userRepository.getAuthenticatedUser()
    val loi =
      locationOfInterestRepository.createLocationOfInterest(geometry, job, surveyId, user, loiName)
    locationOfInterestRepository.applyAndEnqueue(loi.toMutation(Mutation.Type.CREATE, user.id))
    return loi
  }
}
