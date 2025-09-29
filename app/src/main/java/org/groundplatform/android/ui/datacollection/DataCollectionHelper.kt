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
 * Extracted “engine” that contains all mutable logic previously living in DataCollectionViewModel.
 * - Keeps API surface similar so VM can delegate gradually.
 * - Owns flows, sequencing, validation, and draft persistence.
 *
 * Usage: helper.initialize(savedStateHandle, viewModelScope) expose helper.uiState, jobName,
 * loiName from VM delegate calls: onNextClicked(), onPreviousClicked(), saveCurrentState(), etc.
 */
class DataCollectionHelper
@Inject
constructor(
  private val locationOfInterestHelper: LocationOfInterestHelper,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val surveyRepository: SurveyRepository,
) {

  private lateinit var savedStateHandle: SavedStateHandle

  private lateinit var jobId: String
  private var loiId: String? = null
  private val taskDataHandler = TaskDataHandler()
  private lateinit var taskSequenceHandler: TaskSequenceHandler

  /**
   * Initializes this helper with persisted navigation/context state.
   *
   * @throws DataCollectionException.InvalidJobId if the required jobId is absent.
   */
  fun initialize(savedStateHandle: SavedStateHandle) {
    this.savedStateHandle = savedStateHandle
    jobId = savedStateHandle[TASK_JOB_ID_KEY] ?: throw DataCollectionException.InvalidJobId
    loiId = savedStateHandle[TASK_LOI_ID_KEY]
  }

  /**
   * Produces the initial UI state by resolving survey, job, tasks, and LOI name.
   *
   * Contract:
   * - Returns [DataCollectionUiState.Ready] when all dependencies resolve within timeouts.
   * - Returns [DataCollectionUiState.Error] with a stable error code otherwise.
   *
   * Cancellation:
   * - Propagates [CancellationException] to respect coroutine structured concurrency.
   */
  suspend fun initialStateProvider(): DataCollectionUiState =
    try {
      val survey = loadSurveyOrThrow()
      val job = resolveJobOrThrow(survey)

      val tasks = pickTasks(job, loiId)
      initSequenceOrThrow(tasks)

      val initialTaskId = computeInitialTaskId()
      savedStateHandle[TASK_POSITION_ID] = initialTaskId

      val loiName = computeLoiNameOrThrow(survey.id, loiId)

      DataCollectionUiState.Ready(
        surveyId = survey.id,
        job = job,
        loiName = loiName.orEmpty(),
        tasks = tasks,
        isAddLoiFlow = loiId == null,
        currentTaskId = initialTaskId,
        position = taskSequenceHandler.getTaskPosition(initialTaskId),
      )
    } catch (e: DataCollectionException) {
      DataCollectionUiState.Error(e.code, e)
    } catch (c: CancellationException) {
      throw c
    } catch (t: Throwable) {
      DataCollectionUiState.Error(mapThrowableToCode(t), t)
    }

  /**
   * Loads the active [Survey] or throws if not available within the configured timeout.
   *
   * @throws DataCollectionException.SurveyLoadFailed on timeout or missing active survey.
   * @throws CancellationException if the coroutine is cancelled.
   */
  @Suppress("UnusedPrivateMember")
  private suspend fun loadSurveyOrReturnError(): Survey? =
    withTimeoutOrNull(SURVEY_LOAD_TIMEOUT_MILLIS) {
      surveyRepository.activeSurveyFlow.filterNotNull().first()
    }

  private fun resolveJobOrReturnError(survey: Survey): Job? = survey.getJob(jobId)

  private fun pickTasks(job: Job, loiId: String?): List<Task> =
    if (loiId == null) job.tasksSorted else job.tasksSorted.filterNot { it.isAddLoiTask }

  private fun initSequenceOrReturnError(tasks: List<Task>): Boolean {
    taskSequenceHandler = TaskSequenceHandler(tasks, taskDataHandler)
    return taskSequenceHandler.getValidTasks().isNotEmpty()
  }

  /**
   * Picks the first task to show. Precondition: [initSequenceOrThrow] has ensured there is at least
   * one valid task ID.
   */
  private fun computeInitialTaskId(): String {
    val validIds = taskSequenceHandler.getValidTasks().map { it.id }
    return resolveInitialTaskId(validIds = validIds, saved = savedStateHandle[TASK_POSITION_ID])
  }

  private fun resolveInitialTaskId(validIds: List<String>, saved: String?): String =
    saved?.takeIf { it.isNotBlank() && it in validIds } ?: validIds.first()

  private suspend fun computeLoiName(surveyId: String, loiId: String?): String? =
    if (loiId == null) {
      // user is adding a new LOI; pull whatever user typed (may be null/empty)
      savedStateHandle[TASK_LOI_NAME_KEY]
    } else {
      locationOfInterestRepository.getOfflineLoi(surveyId, loiId)?.let {
        locationOfInterestHelper.getDisplayLoiName(it)
      }
    }

  /**
   * Loads the active survey or throws a [DataCollectionException] if none is available within
   * [SURVEY_LOAD_TIMEOUT_MILLIS].
   */
  private suspend fun loadSurveyOrThrow(): Survey =
    withTimeoutOrNull(SURVEY_LOAD_TIMEOUT_MILLIS) {
      surveyRepository.activeSurveyFlow.filterNotNull().first()
    } ?: throw DataCollectionException.SurveyLoadFailed

  /**
   * Resolves the [Job] for the current [jobId] within the given [survey].
   *
   * @throws DataCollectionException.InvalidJobId if not found.
   */
  private fun resolveJobOrThrow(survey: Survey): Job =
    resolveJobOrReturnError(survey) ?: throw DataCollectionException.InvalidJobId

  /**
   * Builds a task sequence and verifies there is at least one valid task.
   *
   * @throws DataCollectionException.NoValidTasks if validation yields an empty sequence.
   */
  private fun initSequenceOrThrow(tasks: List<Task>) {
    if (!initSequenceOrReturnError(tasks)) throw DataCollectionException.NoValidTasks
  }

  /**
   * Computes a user-visible LOI name. For "Add LOI" flows, returns the user-typed value (possibly
   * empty). For existing LOIs, resolves and formats the persisted name.
   *
   * @throws DataCollectionException.LoiNameFailed if resolution fails or no value is available.
   */
  private suspend fun computeLoiNameOrThrow(surveyId: String, loiId: String?): String =
    computeLoiName(surveyId, loiId) ?: throw DataCollectionException.LoiNameFailed

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
    private const val TASK_JOB_ID_KEY = "jobId"
    private const val TASK_LOI_ID_KEY = "locationOfInterestId"
    private const val TASK_LOI_NAME_KEY = "locationOfInterestName"
    private const val TASK_POSITION_ID = "currentTaskId"
    private const val SURVEY_LOAD_TIMEOUT_MILLIS = 3_000L
  }
}
