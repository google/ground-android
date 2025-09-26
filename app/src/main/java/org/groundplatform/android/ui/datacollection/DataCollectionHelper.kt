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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.ui.common.LocationOfInterestHelper
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.date.DateTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.instruction.InstructionTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.number.NumberTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.photo.PhotoTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.time.TimeTaskViewModel

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
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val surveyRepository: SurveyRepository,
) {

  private val _uiState = MutableStateFlow<DataCollectionUiState>(DataCollectionUiState.Loading)
  val uiState: StateFlow<DataCollectionUiState> = _uiState

  private lateinit var scope: CoroutineScope
  private lateinit var savedStateHandle: SavedStateHandle

  private lateinit var jobId: String
  private var loiId: String? = null
  private var shouldLoadFromDraft: Boolean = false
  private val taskDataHandler = TaskDataHandler()
  private lateinit var taskSequenceHandler: TaskSequenceHandler

  fun initialize(savedStateHandle: SavedStateHandle, scope: CoroutineScope) {
    this.savedStateHandle = savedStateHandle
    this.scope = scope

    jobId = requireNotNull(savedStateHandle[TASK_JOB_ID_KEY])
    loiId = savedStateHandle[TASK_LOI_ID_KEY]
    shouldLoadFromDraft = savedStateHandle[TASK_SHOULD_LOAD_FROM_DRAFT] ?: false

    scope.launch { initUiState() }
  }

  private suspend fun initUiState() {
    _uiState.value = initialStateProvider()
  }

  private suspend fun initialStateProvider(): DataCollectionUiState {
    // 1) Load survey
    val survey =
      loadSurveyOrReturnError()
        ?: return DataCollectionUiState.Error("Survey failed to load. Please try again.")

    // 2) Resolve job
    val job =
      resolveJobOrReturnError(survey) ?: return DataCollectionUiState.Error("Invalid jobId: $jobId")

    // 3) Choose tasks (exclude Add-LOI task if an LOI is already selected)
    val tasks = pickTasks(job, loiId)

    // 4) Init sequence + validate
    if (!initSequenceOrReturnError(tasks)) {
      return DataCollectionUiState.Error("No valid tasks for this job.")
    }

    // 5) Position & LOI name
    val initialTaskId = computeInitialTaskId()
    savedStateHandle[TASK_POSITION_ID] = initialTaskId

    val loiName = computeLoiName(survey.id, loiId)

    // 6) Ready
    return DataCollectionUiState.Ready(
      surveyId = survey.id,
      job = job,
      loiName = loiName.orEmpty(),
      tasks = tasks,
      isAddLoiFlow = loiId == null,
      currentTaskId = initialTaskId,
      position = taskSequenceHandler.getTaskPosition(initialTaskId),
    )
  }

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
      withContext(ioDispatcher) {
        locationOfInterestRepository.getOfflineLoi(surveyId, loiId)?.let {
          locationOfInterestHelper.getDisplayLoiName(it)
        }
      }
    }

  companion object {
    private const val TASK_JOB_ID_KEY = "jobId"
    private const val TASK_LOI_ID_KEY = "locationOfInterestId"
    private const val TASK_LOI_NAME_KEY = "locationOfInterestName"
    private const val TASK_POSITION_ID = "currentTaskId"
    private const val TASK_SHOULD_LOAD_FROM_DRAFT = "shouldLoadFromDraft"
    private const val SURVEY_LOAD_TIMEOUT_MILLIS = 3_000L

    fun getViewModelClass(taskType: Task.Type): Class<out AbstractTaskViewModel> =
      when (taskType) {
        Task.Type.TEXT -> TextTaskViewModel::class.java
        Task.Type.MULTIPLE_CHOICE -> MultipleChoiceTaskViewModel::class.java
        Task.Type.PHOTO -> PhotoTaskViewModel::class.java
        Task.Type.NUMBER -> NumberTaskViewModel::class.java
        Task.Type.DATE -> DateTaskViewModel::class.java
        Task.Type.TIME -> TimeTaskViewModel::class.java
        Task.Type.DROP_PIN -> DropPinTaskViewModel::class.java
        Task.Type.DRAW_AREA -> DrawAreaTaskViewModel::class.java
        Task.Type.CAPTURE_LOCATION -> CaptureLocationTaskViewModel::class.java
        Task.Type.INSTRUCTIONS -> InstructionTaskViewModel::class.java
        Task.Type.UNKNOWN -> throw IllegalArgumentException("Unsupported task type: $taskType")
      }
  }
}
