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
package org.groundplatform.android.usecases.submission

import androidx.room.Transaction
import javax.inject.Inject
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.DrawAreaTaskData
import org.groundplatform.android.model.submission.DropPinTaskData
import org.groundplatform.android.model.submission.ValueDelta
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.SubmissionRepository
import timber.log.Timber

class SubmitDataUseCase
@Inject
constructor(
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val submissionRepository: SubmissionRepository,
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
    collectionId: String,
  ) {
    Timber.v("Submitting data for LOI: $selectedLoiId")
    val deltasToSubmit = deltas.toMutableList()
    val submissionLoiId =
      selectedLoiId ?: addLocationOfInterest(surveyId, job, deltasToSubmit, loiName, collectionId)
    submissionRepository.saveSubmission(surveyId, submissionLoiId, deltasToSubmit, collectionId)
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
    collectionId: String,
  ): String {
    val addLoiTask = job.getAddLoiTask() ?: error("AddLoi task missing")
    val addLoiTaskIndex = deltas.indexOfFirst { it.taskId == addLoiTask.id }
    if (addLoiTaskIndex < 0) error("AddLoi task response missing")
    val addLoiTaskValue = deltas.removeAt(addLoiTaskIndex).newTaskData
    val geometry =
      when (addLoiTask.type) {
        Task.Type.DROP_PIN -> (addLoiTaskValue as (DropPinTaskData)).geometry
        Task.Type.DRAW_AREA -> (addLoiTaskValue as (DrawAreaTaskData)).geometry
        else -> error("Invalid AddLoi task")
      }
    return locationOfInterestRepository.saveLoi(geometry, job, surveyId, loiName, collectionId)
  }
}
