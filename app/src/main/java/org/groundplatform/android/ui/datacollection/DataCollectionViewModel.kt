/*
 * Copyright 2022 Google LLC
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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Provider
import kotlin.collections.set
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.groundplatform.android.coroutines.ApplicationScope
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.ValueDelta
import org.groundplatform.android.model.submission.isNotNullOrEmpty
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.persistence.local.room.converter.SubmissionDeltasConverter
import org.groundplatform.android.persistence.uuid.OfflineUuidGenerator
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.SubmissionRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.common.LocationOfInterestHelper
import org.groundplatform.android.ui.common.ViewModelFactory
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
import org.groundplatform.android.usecases.submission.SubmitDataUseCase
import timber.log.Timber

/** View model for the Data Collection fragment. */
@HiltViewModel
class DataCollectionViewModel
@Inject
internal constructor(
  private val viewModelFactory: ViewModelFactory,
  private val locationOfInterestHelper: LocationOfInterestHelper,
  private val popups: Provider<EphemeralPopups>,
  private val submitDataUseCase: SubmitDataUseCase,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val savedStateHandle: SavedStateHandle,
  private val submissionRepository: SubmissionRepository,
  private val offlineUuidGenerator: OfflineUuidGenerator,
  locationOfInterestRepository: LocationOfInterestRepository,
  surveyRepository: SurveyRepository,
) : AbstractViewModel() {

  private val _uiState: MutableStateFlow<UiState?> = MutableStateFlow(null)
  val uiState = _uiState.asStateFlow()

  private val jobId: String = requireNotNull(savedStateHandle[TASK_JOB_ID_KEY])
  private val loiId: String? = savedStateHandle[TASK_LOI_ID_KEY]

  /** True iff the user is expected to produce a new LOI in the current data collection flow. */
  private val isAddLoiFlow = loiId == null

  private var shouldLoadFromDraft: Boolean = savedStateHandle[TASK_SHOULD_LOAD_FROM_DRAFT] ?: false
  private var draftDeltas: List<ValueDelta>? = null

  private val activeSurvey: Survey = runBlocking {
    try {
      withTimeout(SURVEY_LOAD_TIMEOUT_MILLIS) {
        surveyRepository.activeSurveyFlow.filterNotNull().first()
      }
    } catch (e: TimeoutCancellationException) {
      Timber.e(e, "Failed to get survey due to timeout")
      throw e
    }
  }

  private val job: Job = activeSurvey.getJob(jobId) ?: error("couldn't retrieve job for $jobId")
  private var customLoiName: String?
    get() = savedStateHandle[TASK_LOI_NAME_KEY]
    set(value) {
      savedStateHandle[TASK_LOI_NAME_KEY] = value
    }

  // LOI creation task is included only on "new data collection site" flow..
  val tasks: List<Task> =
    if (isAddLoiFlow) job.tasksSorted else job.tasksSorted.filterNot { it.isAddLoiTask }

  val surveyId: String = requireNotNull(surveyRepository.activeSurvey?.id)

  val jobName: StateFlow<String> =
    MutableStateFlow(job.name ?: "").stateIn(viewModelScope, SharingStarted.Lazily, "")

  val loiName: StateFlow<String?> =
    (if (loiId == null) {
        // User supplied LOI name during LOI creation task. Use to save the LOI name later.
        savedStateHandle.getStateFlow(TASK_LOI_NAME_KEY, "")
      } else
      // LOI name pulled from LOI properties, if it exists.
      flow {
          locationOfInterestRepository.getOfflineLoi(surveyId, loiId)?.let {
            val label = locationOfInterestHelper.getDisplayLoiName(it)
            emit(label)
          }
        })
      .stateIn(viewModelScope, SharingStarted.Lazily, "")

  val loiNameDialogOpen: MutableState<Boolean> = mutableStateOf(false)

  private val taskViewModels: MutableStateFlow<MutableMap<String, AbstractTaskViewModel>> =
    MutableStateFlow(mutableMapOf())

  private val taskDataHandler = TaskDataHandler()
  private val taskSequenceHandler = TaskSequenceHandler(tasks, taskDataHandler)

  // Tracks the current task ID to compute the position in the list of tasks for the current job.
  private val currentTaskId: StateFlow<String> = savedStateHandle.getStateFlow(TASK_POSITION_ID, "")

  init {
    // Set current task's ID for new task submissions.
    if (currentTaskId.value == "") {
      savedStateHandle[TASK_POSITION_ID] = taskSequenceHandler.getValidTasks().first().id
    }

    // Invalidates the cache if any of the task's data is updated.
    viewModelScope.launch {
      taskDataHandler.dataState.collectLatest { taskSequenceHandler.invalidateCache() }
    }
  }

  /** Returns the ID of the user visible task. */
  fun getCurrentTaskId(): String {
    val taskId = currentTaskId.value
    check(taskId.isNotBlank()) { "Task ID can't be blank" }
    return taskId
  }

  fun setLoiName(name: String) {
    customLoiName = name
  }

  private fun getDraftDeltas(): List<ValueDelta> {
    if (!shouldLoadFromDraft) return listOf()
    if (draftDeltas != null) return draftDeltas as List<ValueDelta>

    val serializedDraftValues = savedStateHandle[TASK_DRAFT_VALUES] ?: ""
    if (serializedDraftValues.isEmpty()) {
      Timber.e("Attempting load from draft submission failed, not found")
      return listOf()
    }

    draftDeltas = SubmissionDeltasConverter.fromString(job, serializedDraftValues)
    return draftDeltas as List<ValueDelta>
  }

  private fun getValueFromDraft(task: Task): TaskData? {
    for ((taskId, taskType, value) in getDraftDeltas()) {
      if (taskId == task.id && taskType == task.type) {
        Timber.d("Value $value found for task $task")
        return value
      }
    }
    Timber.w("Value not found for task $task")
    return null
  }

  fun getTaskViewModel(taskId: String): AbstractTaskViewModel? {
    val viewModels = taskViewModels.value

    val task = tasks.first { it.id == taskId }

    if (viewModels.containsKey(taskId)) {
      return viewModels[taskId]
    }

    return try {
      val viewModel = viewModelFactory.create(getViewModelClass(task.type))
      taskViewModels.value[task.id] = viewModel

      val taskData: TaskData? = if (shouldLoadFromDraft) getValueFromDraft(task) else null
      viewModel.initialize(job, task, taskData)
      taskDataHandler.setData(task, taskData)
      viewModel
    } catch (e: Exception) {
      Timber.e("ignoring task with invalid type: $task.type")
      null
    }
  }

  /** Moves back to the previous task in the sequence if the current value is valid or empty. */
  fun onPreviousClicked(taskViewModel: AbstractTaskViewModel) {
    val task = taskViewModel.task
    val taskValue = taskViewModel.taskTaskData.value

    taskValue
      ?.takeIf { it.isNotNullOrEmpty() } // Skip validation if the task is empty
      ?.let {
        taskViewModel.validate()?.let {
          popups.get().ErrorPopup().show(it)
          return
        }
      }

    taskDataHandler.setData(task, taskValue)
    moveToPreviousTask()
  }

  /**
   * Validates the user's input and displays an error if the user input was invalid. Progresses to
   * the next Data Collection screen if the user input was valid.
   */
  fun onNextClicked(taskViewModel: AbstractTaskViewModel) {
    taskViewModel.validate()?.let {
      popups.get().ErrorPopup().show(it)
      return
    }

    val task = taskViewModel.task
    val taskValue = taskViewModel.taskTaskData
    taskDataHandler.setData(task, taskValue.value)

    if (!isLastPosition(task.id)) {
      moveToNextTask()
    } else {
      clearDraft()
      saveChanges(getDeltas())
      _uiState.update { UiState.TaskSubmitted }
    }
  }

  /** Persists the current UI state locally which can be resumed whenever the app re-opens. */
  fun saveCurrentState() {
    val taskId = getCurrentTaskId()
    val viewModel = getTaskViewModel(taskId) ?: error("ViewModel not found for task $taskId")

    viewModel.validate()?.let {
      Timber.d("Ignoring task $taskId with invalid data")
      return
    }

    taskDataHandler.setData(viewModel.task, viewModel.taskTaskData.value)
    savedStateHandle[TASK_POSITION_ID] = taskId
    saveDraft(taskId)
  }

  /** Retrieves a list of [ValueDelta] for tasks that are part of the current sequence. */
  private fun getDeltas(): List<ValueDelta> {
    val tasksDataMap = taskDataHandler.dataState.value
    val validTasks = taskSequenceHandler.getValidTasks()
    return tasksDataMap
      .filterKeys { validTasks.contains(it) }
      .map { (task, taskData) -> ValueDelta(task.id, task.type, taskData) }
  }

  /** Persists the changes locally and enqueues a worker to sync with remote datastore. */
  private fun saveChanges(deltas: List<ValueDelta>) {
    externalScope.launch(ioDispatcher) {
      val collectionId = offlineUuidGenerator.generateUuid()
      submitDataUseCase.invoke(loiId, job, surveyId, deltas, customLoiName, collectionId)
    }
  }

  /** Persists the collected data as draft to local storage. */
  private fun saveDraft(taskId: String) {
    val deltas = getDeltas()

    // Prevent saving draft if the task is submitted or there are no deltas.
    if (_uiState.value == UiState.TaskSubmitted || deltas.isEmpty()) {
      return
    }

    externalScope.launch(ioDispatcher) {
      submissionRepository.saveDraftSubmission(
        jobId = jobId,
        loiId = loiId,
        surveyId = surveyId,
        deltas = deltas,
        loiName = customLoiName,
        currentTaskId = taskId,
      )
    }
  }

  /** Clears all persisted drafts from local storage. */
  fun clearDraft() {
    externalScope.launch(ioDispatcher) { submissionRepository.deleteDraftSubmission() }
  }

  private fun moveToNextTask() {
    val taskId = taskSequenceHandler.getNextTask(getCurrentTaskId())
    moveToTask(taskId)
  }

  fun moveToPreviousTask() {
    val taskId = taskSequenceHandler.getPreviousTask(getCurrentTaskId())
    moveToTask(taskId)
  }

  private fun moveToTask(taskId: String) {
    savedStateHandle[TASK_POSITION_ID] = taskId

    // Save collected data as draft
    clearDraft()
    saveDraft(taskId)

    _uiState.update { UiState.TaskUpdated(getTaskPosition(taskId)) }
  }

  fun getTaskPosition(taskId: String) = taskSequenceHandler.getTaskPosition(taskId)

  /** Returns true if the given [taskId] is first task in the sequence of displayed tasks. */
  fun isFirstPosition(taskId: String): Boolean = taskSequenceHandler.isFirstPosition(taskId)

  /** Returns true if the given [taskId] is last task in the sequence of displayed tasks. */
  fun isLastPosition(taskId: String): Boolean = taskSequenceHandler.isLastPosition(taskId)

  /**
   * Returns true if the given [task] with [newValue] would be last in the sequence of displayed
   * tasks. Required for handling conditional tasks, see #2394.
   */
  fun isLastPositionWithValue(task: Task, newValue: TaskData?): Boolean {
    if (taskDataHandler.getData(task) == newValue) {
      // Reuse the existing task sequence if the value has already been saved (i.e. after pressing
      // "Next" and going back).
      return isLastPosition(task.id)
    }

    return taskSequenceHandler.checkIfTaskIsLastWithValue(taskValueOverride = task.id to newValue)
  }

  companion object {
    private const val TASK_JOB_ID_KEY = "jobId"
    private const val TASK_LOI_ID_KEY = "locationOfInterestId"
    private const val TASK_LOI_NAME_KEY = "locationOfInterestName"
    private const val TASK_POSITION_ID = "currentTaskId"
    private const val TASK_SHOULD_LOAD_FROM_DRAFT = "shouldLoadFromDraft"
    private const val TASK_DRAFT_VALUES = "draftValues"
    private const val SURVEY_LOAD_TIMEOUT_MILLIS = 3000L

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
