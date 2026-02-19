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
package org.groundplatform.android.ui.datacollection

import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.ui.common.LocationOfInterestHelper

/**
 * DataCollectionInitializer
 *
 * Focused initializer that computes the FIRST renderable UI state for Data Collection.
 *
 * Responsibilities:
 * - Load active survey (with timeout).
 * - Resolve job by id.
 * - Pick displayable tasks (exclude Add-LOI when LOI exists).
 * - Choose initial task id from SavedStateHandle (if valid) or first task.
 * - Compute a simple [TaskPosition] from list order.
 * - Resolve a user-visible LOI name (typed for Add-LOI, formatted for existing LOI).
 */
class DataCollectionInitializer
@Inject
constructor(
  private val locationOfInterestHelper: LocationOfInterestHelper,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val surveyRepository: SurveyRepository,
) {

  /**
   * Computes the initial [DataCollectionUiState] without building any sequence.
   *
   * Reads from [savedStateHandle]:
   * - [TASK_POSITION_ID]: previously visited task id (optional).
   * - [TASK_LOI_NAME_KEY]: user-typed LOI name for Add-LOI flows (optional).
   */
  suspend fun initialize(
    savedStateHandle: SavedStateHandle,
    jobId: String,
    loiId: String?,
    loiName: String?,
  ): DataCollectionUiState =
    try {
      val survey = loadSurveyOrThrow()
      val job = resolveJobOrThrow(survey, jobId)
      val tasks = pickTasks(job, loiId)
      if (tasks.isEmpty()) throw DataCollectionException.NoValidTasks

      val savedTaskId: String? = savedStateHandle[TASK_POSITION_ID]
      val currentTaskId =
        resolveInitialTaskId(tasks, savedTaskId)
          ?: throw DataCollectionException.Wrapped(
            DataCollectionErrorCode.INITIAL_TASK_RESOLUTION_FAILED,
            IllegalStateException("No valid initial task id"),
          )

      // Persist the chosen initial position for downstream consumers.
      savedStateHandle[TASK_POSITION_ID] = currentTaskId

      val position =
        computeListPosition(tasks, currentTaskId)
          ?: throw DataCollectionException.Wrapped(
            DataCollectionErrorCode.INITIAL_TASK_RESOLUTION_FAILED,
            IllegalStateException("Could not compute TaskPosition for $currentTaskId"),
          )

      val loiName = computeLoiName(survey.id, loiId, loiName)

      DataCollectionUiState.Ready(
        surveyId = survey.id,
        job = job,
        loiName = loiName,
        tasks = tasks,
        isAddLoiFlow = loiId == null,
        currentTaskId = currentTaskId,
        position = position,
      )
    } catch (e: DataCollectionException) {
      DataCollectionUiState.Error(e.code, e)
    } catch (c: CancellationException) {
      throw c
    } catch (t: Throwable) {
      DataCollectionUiState.Error(mapThrowableToCode(t), t)
    }

  private suspend fun loadSurveyOrThrow(): Survey =
    withTimeoutOrNull(SURVEY_LOAD_TIMEOUT_MILLIS) {
      surveyRepository.activeSurveyFlow.filterNotNull().first()
    } ?: throw DataCollectionException.SurveyLoadFailed

  private fun resolveJobOrThrow(survey: Survey, jobId: String): Job =
    survey.getJob(jobId) ?: throw DataCollectionException.InvalidJobId

  /**
   * Returns the displayable tasks:
   * - Add-LOI flow (loiId == null): include all tasks.
   * - Existing LOI: exclude Add-LOI task.
   */
  private fun pickTasks(job: Job, loiId: String?): List<Task> =
    if (loiId == null) job.tasksSorted else job.tasksSorted.filterNot { it.isAddLoiTask }

  /**
   * Choose initial task:
   * - Use [saved] if it exists within [tasks].
   * - Otherwise, first task in list.
   */
  private fun resolveInitialTaskId(tasks: List<Task>, saved: String?): String? {
    val validIds = tasks.map { it.id }.toSet()
    return saved?.takeIf { it in validIds } ?: tasks.firstOrNull()?.id
  }

  /**
   * Computes a simple [TaskPosition] derived from list order (index/first/last). This avoids any
   * sequence logic during initialization.
   */
  private fun computeListPosition(tasks: List<Task>, currentTaskId: String): TaskPosition? {
    val idx = tasks.indexOfFirst { it.id == currentTaskId }
    if (idx < 0) return null
    // Assuming TaskPosition provides a constructor or factory with these fields.
    return TaskPosition(absoluteIndex = idx, relativeIndex = idx, sequenceSize = tasks.size)
  }

  /**
   * Computes a user-visible LOI name:
   * - Add-LOI flow: uses [typedName] (may be empty but must not be null).
   * - Existing LOI: loads and formats via [locationOfInterestHelper].
   *
   * @throws DataCollectionException.LoiNameFailed when unavailable.
   */
  private suspend fun computeLoiName(surveyId: String, loiId: String?, typedName: String?): String =
    if (loiId == null) {
      typedName.orEmpty()
    } else {
      locationOfInterestRepository
        .getOfflineLoi(surveyId, loiId)
        ?.let { locationOfInterestHelper.getDisplayLoiName(it) }
        .orEmpty()
    }

  private fun mapThrowableToCode(t: Throwable): DataCollectionErrorCode =
    when (t) {
      is CancellationException -> throw t
      is java.net.SocketTimeoutException,
      is java.net.UnknownHostException -> DataCollectionErrorCode.NETWORK
      is java.io.IOException -> DataCollectionErrorCode.IO
      is SecurityException -> DataCollectionErrorCode.PERMISSION_DENIED
      else -> DataCollectionErrorCode.UNKNOWN
    }

  companion object {
    private const val TASK_POSITION_ID = "currentTaskId"
    private const val SURVEY_LOAD_TIMEOUT_MILLIS = 3_000L
  }
}
