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
package com.google.android.ground.ui.datacollection

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.domain.usecases.submission.SubmitDataUseCase
import com.google.android.ground.model.Survey
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.room.converter.SubmissionDeltasConverter
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.LocationOfInterestHelper
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.date.DateTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.location.CaptureLocationTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.number.NumberTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.photo.PhotoTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.point.DropPinTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.polygon.DrawAreaTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.text.TextTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.time.TimeTaskViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

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

  private val activeSurvey: Survey = requireNotNull(surveyRepository.activeSurvey)
  private val job: Job = activeSurvey.getJob(jobId) ?: error("couldn't retrieve job for $jobId")
  private var customLoiName: String?
    get() = savedStateHandle[TASK_LOI_NAME_KEY]
    set(value) {
      savedStateHandle[TASK_LOI_NAME_KEY] = value
    }

  // LOI creation task is included only on "new data collection site" flow..
  private val tasks: List<Task> =
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

  private val data: MutableMap<Task, TaskData?> = LinkedHashMap()

  // Tracks the current task ID to compute the position in the list of tasks for the current job.
  private val currentTaskId: StateFlow<String> = savedStateHandle.getStateFlow(TASK_POSITION_ID, "")

  lateinit var submissionId: String

  private val taskSequenceHandler: TaskSequenceHandler =
    TaskSequenceHandlerImpl(tasks, ::shouldIncludeTask)

  init {
    if (currentTaskId.value == "") {
      // Set current task's ID for new task submissions.
      savedStateHandle[TASK_POSITION_ID] = taskSequenceHandler.getTaskSequence().first().id
    }

    check(currentTaskId.value.isNotBlank()) { "Task ID can't be blank" }
    _uiState.update { UiState.TaskListAvailable(tasks, getTaskPosition(currentTaskId.value)) }
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

    // Skip validation if the task is empty
    if (taskValue.isNullOrEmpty()) {
      data[task] = taskValue
      moveToPreviousTask()
      return
    }

    val validationError = taskViewModel.validate()
    if (validationError != null) {
      popups.get().ErrorPopup().show(validationError)
      return
    }

    data[task] = taskValue
    moveToPreviousTask()
  }

  /**
   * Validates the user's input and displays an error if the user input was invalid. Progresses to
   * the next Data Collection screen if the user input was valid.
   */
  fun onNextClicked(taskViewModel: AbstractTaskViewModel) {
    val validationError = taskViewModel.validate()
    if (validationError != null) {
      popups.get().ErrorPopup().show(validationError)
      return
    }

    data[taskViewModel.task] = taskViewModel.taskTaskData.value

    if (!isLastPosition(currentTaskId.value)) {
      moveToNextTask()
    } else {
      clearDraft()
      saveChanges(getDeltas())
      _uiState.update { UiState.TaskSubmitted }
    }
  }

  /** Persists the current UI state locally which can be resumed whenever the app re-opens. */
  fun saveCurrentState() {
    val taskId = currentTaskId.value
    val viewModel = getTaskViewModel(taskId) ?: error("ViewModel not found for task $taskId")

    val validationError = viewModel.validate()
    if (validationError != null) {
      Timber.d("Ignoring task $taskId with invalid data: $validationError")
      return
    }

    data[viewModel.task] = viewModel.taskTaskData.value
    savedStateHandle[TASK_POSITION_ID] = taskId
    saveDraft(taskId)
  }

  private fun getDeltas(): List<ValueDelta> =
    // Filter deltas to valid tasks.
    data
      .filter { (task) -> task in taskSequenceHandler.getTaskSequence() }
      .map { (task, value) -> ValueDelta(task.id, task.type, value) }

  /** Persists the changes locally and enqueues a worker to sync with remote datastore. */
  private fun saveChanges(deltas: List<ValueDelta>) {
    externalScope.launch(ioDispatcher) {
      val collectionId = offlineUuidGenerator.generateUuid()
      submitDataUseCase.invoke(loiId, job, surveyId, deltas, customLoiName, collectionId)
    }
  }

  /** Persists the collected data as draft to local storage. */
  private fun saveDraft(taskId: String) {
    externalScope.launch(ioDispatcher) {
      submissionRepository.saveDraftSubmission(
        jobId = jobId,
        loiId = loiId,
        surveyId = surveyId,
        deltas = getDeltas(),
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
    val taskId = taskSequenceHandler.getNextTask(currentTaskId.value)
    moveToTask(taskId)
  }

  fun moveToPreviousTask() {
    val taskId = taskSequenceHandler.getPreviousTask(currentTaskId.value)
    moveToTask(taskId)
  }

  /** Displays the task at the relative position to the current one. */
  private fun moveToTask(taskId: String) {
    savedStateHandle[TASK_POSITION_ID] = taskId

    // Save collected data as draft
    clearDraft()
    saveDraft(taskId)

    _uiState.update { UiState.TaskUpdated(getTaskPosition(taskId)) }
  }

  private fun getTaskPosition(taskId: String): TaskPosition =
    taskSequenceHandler.getTaskPosition(taskId)

  fun isFirstPosition(taskId: String): Boolean = taskSequenceHandler.isFirstPosition(taskId)

  fun isLastPosition(taskId: String, value: TaskData?): Boolean =
    taskSequenceHandler.isLastPosition(taskId, value)

  fun isLastPosition(taskId: String): Boolean = taskSequenceHandler.isLastPosition(taskId)

  /** Evaluates the task condition against the current inputs. */
  private fun shouldIncludeTask(
    task: Task,
    taskValueOverride: Pair<String, TaskData?>? = null,
  ): Boolean {
    val condition = task.condition ?: return true
    return condition.fulfilledBy(
      data
        .mapNotNull { (task, value) -> value?.let { task.id to it } }
        .let { pairs ->
          if (taskValueOverride != null) {
            if (taskValueOverride.second == null) {
              // Remove pairs with the testTaskId if testValue is null.
              pairs.filterNot { it.first == taskValueOverride.first }
            } else {
              // Override any task IDs with the test values.
              pairs + (taskValueOverride.first to taskValueOverride.second!!)
            }
          } else {
            pairs
          }
        }
        .toMap()
    )
  }

  companion object {
    private const val TASK_JOB_ID_KEY = "jobId"
    private const val TASK_LOI_ID_KEY = "locationOfInterestId"
    private const val TASK_LOI_NAME_KEY = "locationOfInterestName"
    private const val TASK_POSITION_ID = "currentTaskId"
    private const val TASK_SHOULD_LOAD_FROM_DRAFT = "shouldLoadFromDraft"
    private const val TASK_DRAFT_VALUES = "draftValues"

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
        Task.Type.UNKNOWN -> throw IllegalArgumentException("Unsupported task type: $taskType")
      }
  }
}
