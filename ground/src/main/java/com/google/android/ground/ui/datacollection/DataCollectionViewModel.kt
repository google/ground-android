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
import com.google.android.ground.model.task.Condition
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
import javax.inject.Inject
import javax.inject.Provider
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.abs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
  var uiState = _uiState.asStateFlow().stateIn(viewModelScope, SharingStarted.Lazily, null)

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
          val loi = locationOfInterestRepository.getOfflineLoi(surveyId, loiId)
          val label = locationOfInterestHelper.getDisplayLoiName(loi)
          emit(label)
        })
      .stateIn(viewModelScope, SharingStarted.Lazily, "")

  val loiNameDialogOpen: MutableState<Boolean> = mutableStateOf(false)

  private val taskViewModels: MutableStateFlow<MutableMap<String, AbstractTaskViewModel>> =
    MutableStateFlow(mutableMapOf())

  private val data: MutableMap<Task, TaskData?> = LinkedHashMap()

  // Tracks the current task ID to compute the position in the list of tasks for the current job.
  private val currentTaskId: StateFlow<String> =
    savedStateHandle.getStateFlow(TASK_POSITION_ID, tasks.firstOrNull()?.id ?: "")

  lateinit var submissionId: String

  suspend fun init() {
    _uiState.emit(UiState.TaskListAvailable(tasks, getTaskPosition()))
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
  suspend fun onPreviousClicked(taskViewModel: AbstractTaskViewModel) {
    check(getPositionInTaskSequence().first != 0)

    val task = taskViewModel.task
    val taskValue = taskViewModel.taskTaskData.firstOrNull()

    // Skip validation if the task is empty
    if (taskValue.isNullOrEmpty()) {
      data[task] = taskValue
      step(-1)
      return
    }

    val validationError = taskViewModel.validate()
    if (validationError != null) {
      popups.get().ErrorPopup().show(validationError)
      return
    }

    data[task] = taskValue
    step(-1)
  }

  /**
   * Validates the user's input and displays an error if the user input was invalid. Progresses to
   * the next Data Collection screen if the user input was valid.
   */
  suspend fun onNextClicked(taskViewModel: AbstractTaskViewModel) {
    val validationError = taskViewModel.validate()
    if (validationError != null) {
      popups.get().ErrorPopup().show(validationError)
      return
    }

    data[taskViewModel.task] = taskViewModel.taskTaskData.value

    if (!isLastPosition()) {
      step(1)
    } else {
      clearDraft()
      saveChanges(getDeltas())
      _uiState.emit(UiState.TaskSubmitted)
    }
  }

  fun saveCurrentState() {
    getTaskViewModel(currentTaskId.value)?.let {
      if (!data.containsKey(it.task)) {
        val validationError = it.validate()
        if (validationError != null) {
          return
        }

        data[it.task] = it.taskTaskData.value
        savedStateHandle[TASK_POSITION_ID] = it.task.id
        saveDraft()
      }
    }
  }

  private fun getDeltas(): List<ValueDelta> =
    // Filter deltas to valid tasks.
    data
      .filter { (task) -> task in getTaskSequence() }
      .map { (task, value) -> ValueDelta(task.id, task.type, value) }

  /** Persists the changes locally and enqueues a worker to sync with remote datastore. */
  private fun saveChanges(deltas: List<ValueDelta>) {
    val collectionId = offlineUuidGenerator.generateUuid()
    externalScope.launch(ioDispatcher) {
      submitDataUseCase.invoke(loiId, job, surveyId, deltas, customLoiName, collectionId)
    }
  }

  private fun getAbsolutePosition(): Int {
    if (currentTaskId.value == "") {
      return 0
    }
    return tasks.indexOf(tasks.first { it.id == currentTaskId.value })
  }

  /** Persists the collected data as draft to local storage. */
  private fun saveDraft() {
    externalScope.launch(ioDispatcher) {
      submissionRepository.saveDraftSubmission(
        jobId = jobId,
        loiId = loiId,
        surveyId = surveyId,
        deltas = getDeltas(),
        loiName = customLoiName,
      )
    }
  }

  /** Clears all persisted drafts from local storage. */
  fun clearDraft() {
    externalScope.launch(ioDispatcher) { submissionRepository.deleteDraftSubmission() }
  }

  /**
   * Get the current index within the computed task sequence, and the number of tasks in the
   * sequence, e.g (0, 2) means the first task of 2.
   */
  private fun getPositionInTaskSequence(): Pair<Int, Int> {
    var currentIndex = 0
    var size = 0
    getTaskSequence().forEachIndexed { index, task ->
      if (task.id == currentTaskId.value) {
        currentIndex = index
      }
      size++
    }
    return currentIndex to size
  }

  /** Returns the index of the task ID, or -1 if null or not found. */
  private fun getIndexOfTask(taskId: String?) =
    if (taskId == null) {
      -1
    } else {
      tasks.indexOfFirst { it.id == taskId }
    }

  /**
   * Retrieves the current task sequence given the inputs and conditions set on the tasks. Setting a
   * start ID will always generate a sequence with the start ID as the first element, and if
   * reversed is set, will generate the previous tasks from there.
   */
  private fun getTaskSequence(
    startId: String? = null,
    reversed: Boolean = false,
    taskValueOverride: Pair<String, TaskData?>? = null,
  ): Sequence<Task> {
    if (tasks.isEmpty()) {
      error("Can't generate sequence for empty task list")
    }
    val startIndex =
      getIndexOfTask(startId).let {
        if (it < 0) {
          // Default to 0 if startId is not found or is null.
          if (startId != null) Timber.w("startId, $startId, was not found. Defaulting to 0")
          0
        } else {
          it
        }
      }
    return if (reversed) {
        tasks.subList(0, startIndex + 1).reversed()
      } else {
        tasks.subList(startIndex, tasks.size)
      }
      .let { tasks ->
        tasks.asSequence().filter {
          it.condition == null || evaluateCondition(it.condition, taskValueOverride)
        }
      }
  }

  /** Displays the task at the relative position to the current one. Supports negative steps. */
  suspend fun step(stepCount: Int) {
    val reverse = stepCount < 0
    val task =
      getTaskSequence(startId = currentTaskId.value, reversed = reverse)
        .take(abs(stepCount) + 1)
        .last()
    savedStateHandle[TASK_POSITION_ID] = task.id

    // Save collected data as draft
    clearDraft()
    saveDraft()

    _uiState.emit(UiState.TaskUpdated(getTaskPosition()))
  }

  private fun getTaskPosition(): TaskPosition {
    val (index, size) = getPositionInTaskSequence()
    return TaskPosition(
      absoluteIndex = getAbsolutePosition(),
      relativeIndex = index,
      sequenceSize = size,
    )
  }

  /** Returns true if the given [taskId] is first in the sequence of displayed tasks. */
  fun isFirstPosition(taskId: String): Boolean = taskId == getTaskSequence().first().id

  /**
   * Returns true if the given [taskId] with task data would be last in sequence. Defaults to the
   * current active task if not set. Useful for handling conditional tasks, see #2394.
   */
  fun checkLastPositionWithTaskData(taskId: String? = null, value: TaskData?): Boolean =
    (taskId ?: currentTaskId.value) ==
      getTaskSequence(taskValueOverride = (taskId ?: currentTaskId.value) to value).last().id

  /** Returns true if the given [taskId] is last if set, or the current active task. */
  fun isLastPosition(taskId: String? = null): Boolean =
    (taskId ?: currentTaskId.value) == getTaskSequence().last().id

  /** Evaluates the task condition against the current inputs. */
  private fun evaluateCondition(
    condition: Condition,
    taskValueOverride: Pair<String, TaskData?>? = null,
  ): Boolean =
    condition.fulfilledBy(
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
