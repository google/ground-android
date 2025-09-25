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

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.groundplatform.android.coroutines.ApplicationScope
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.data.local.room.converter.SubmissionDeltasConverter
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.ValueDelta
import org.groundplatform.android.model.submission.isNotNullOrEmpty
import org.groundplatform.android.model.task.Task
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
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val surveyRepository: SurveyRepository,
) : AbstractViewModel() {

  private val _uiState = MutableStateFlow<DataCollectionUiState>(DataCollectionUiState.Loading)
  val uiState: StateFlow<DataCollectionUiState> = _uiState

  private val jobId: String = requireNotNull(savedStateHandle[TASK_JOB_ID_KEY])
  private val loiId: String? = savedStateHandle[TASK_LOI_ID_KEY]
  /** True if the user is expected to produce a new LOI in the current data collection flow. */
  private val isAddLoiFlow = loiId == null
  private var shouldLoadFromDraft: Boolean = savedStateHandle[TASK_SHOULD_LOAD_FROM_DRAFT] ?: false

  val jobName: StateFlow<String> =
    uiState
      .map { (it as? DataCollectionUiState.Ready)?.job?.name.orEmpty() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

  @OptIn(ExperimentalCoroutinesApi::class)
  val loiName: StateFlow<String?> =
    uiState
      .flatMapLatest { s ->
        val ready = s as? DataCollectionUiState.Ready
        when {
          ready == null -> flowOf<String?>(null)

          loiId == null -> savedStateHandle.getStateFlow(TASK_LOI_NAME_KEY, "")

          else ->
            flow {
              val offline =
                withContext(ioDispatcher) {
                  locationOfInterestRepository.getOfflineLoi(ready.surveyId, loiId)
                }
              emit(offline?.let { locationOfInterestHelper.getDisplayLoiName(it) })
            }
        }
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, null)

  val loiNameDialogOpen = mutableStateOf(false)

  private val taskViewModels = MutableStateFlow(mutableMapOf<String, AbstractTaskViewModel>())
  private val taskDataHandler = TaskDataHandler()
  // Initialized later when job/tasks are ready
  private lateinit var taskSequenceHandler: TaskSequenceHandler

  @Volatile private var draftCache: List<ValueDelta>? = null
  @Volatile private var draftMapCache: Map<Pair<String, Task.Type>, TaskData?>? = null
  private val draftLock = Any()

  init {
    viewModelScope.launch { initUiState() }
  }

  private suspend fun initUiState() {
    _uiState.value = computeInitialUiState()
  }

  private suspend fun computeInitialUiState(): DataCollectionUiState {
    var errorMessage: String? = null

    // 1) Load survey (with timeout)
    val survey =
      withTimeoutOrNull(SURVEY_LOAD_TIMEOUT_MILLIS) {
          surveyRepository.activeSurveyFlow.filterNotNull().first()
        }
        .also { if (it == null) errorMessage = "Survey failed to load. Please try again." }

    // 2) Resolve job
    val job =
      survey?.getJob(jobId).also {
        if (survey != null && it == null) errorMessage = "Invalid jobId: $jobId"
      }

    // 3) Choose tasks
    val tasks =
      if (job != null) {
        if (isAddLoiFlow) job.tasksSorted else job.tasksSorted.filterNot { it.isAddLoiTask }
      } else {
        emptyList()
      }

    // 4) Sequence + validation (only if we have a job)
    if (job != null) {
      taskSequenceHandler = TaskSequenceHandler(tasks, taskDataHandler)
      val valid = taskSequenceHandler.getValidTasks()
      if (valid.isEmpty()) errorMessage = "No valid tasks for this job."
    }

    // 5) Error short-circuit (single return for the whole function)
    errorMessage?.let {
      return DataCollectionUiState.Error(it)
    }

    // 6) Happy path → compute initial task id and build Ready state
    val valid = taskSequenceHandler.getValidTasks()
    val initialTaskId =
      resolveInitialTaskId(
        validIds = valid.map { it.id },
        saved = savedStateHandle[TASK_POSITION_ID],
      )
    savedStateHandle[TASK_POSITION_ID] = initialTaskId

    return DataCollectionUiState.Ready(
      surveyId = requireNotNull(survey!!.id),
      job = job!!,
      tasks = tasks,
      isAddLoiFlow = isAddLoiFlow,
      currentTaskId = initialTaskId,
      position = taskSequenceHandler.getTaskPosition(initialTaskId),
    )
  }

  fun setLoiName(name: String) {
    savedStateHandle[TASK_LOI_NAME_KEY] = name
  }

  fun getTaskViewModel(taskId: String): AbstractTaskViewModel? = withReady { state ->
    taskViewModels.value[taskId]?.let {
      return it
    }

    val task =
      state.tasks.firstOrNull { it.id == taskId }
        ?: error(
          "Task not found. taskId=$taskId, jobId=$jobId, loiId=$loiId, surveyId=${state.surveyId}"
        )

    val viewModel =
      try {
        viewModelFactory.create(getViewModelClass(task.type))
      } catch (e: Exception) {
        Timber.e(e, "Ignoring task with invalid type: ${task.type}")
        null
      }

    viewModel?.let { created ->
      // Load draft value synchronously (pure CPU parse); safe and avoids lateinit issues
      val taskData = if (shouldLoadFromDraft) getValueFromDraft(state.job, task) else null
      created.initialize(state.job, task, taskData)
      taskDataHandler.setData(task, taskData)
      taskViewModels.value[task.id] = created
    }
    viewModel
  }

  /** Moves back to the previous task in the sequence if the current value is valid or empty. */
  fun onPreviousClicked(taskViewModel: AbstractTaskViewModel) = withReady { _ ->
    val task = taskViewModel.task
    val taskValue = taskViewModel.taskTaskData.value

    // Validate only when there is user input
    val validationError =
      if (taskValue?.isNotNullOrEmpty() == true) taskViewModel.validate() else null

    if (validationError != null) {
      popups.get().ErrorPopup().show(validationError)
    } else {
      taskDataHandler.setData(task, taskValue)
      taskSequenceHandler.invalidateCache()
      moveToPreviousTask()
    }
  }

  /**
   * Validates the user's input and displays an error if the user input was invalid. Progresses to
   * the next Data Collection screen if the user input was valid.
   */
  fun onNextClicked(taskViewModel: AbstractTaskViewModel) = withReady { st ->
    validateOrShow(taskViewModel) {
      val task = taskViewModel.task
      val value = taskViewModel.taskTaskData.value
      taskDataHandler.setData(task, value)
      taskSequenceHandler.invalidateCache()

      if (!taskSequenceHandler.isLastPosition(task.id)) {
        moveToNextTask()
      } else {
        clearDraft()
        saveChanges(st, getDeltas())
        _uiState.value = DataCollectionUiState.TaskSubmitted
      }
    }
  }

  /** Persists the current UI state locally which can be resumed whenever the app re-opens. */
  fun saveCurrentState() {
    withReadyOrNull { state ->
      val taskId = state.currentTaskId
      getTaskViewModel(taskId)?.let { vm ->
        taskDataHandler.setData(vm.task, vm.taskTaskData.value)
        savedStateHandle[TASK_POSITION_ID] = taskId
        saveDraft(taskId)
      }
    }
  }

  /** Clears all persisted drafts from local storage. */
  fun clearDraft() {
    viewModelScope.launch(ioDispatcher) { submissionRepository.deleteDraftSubmission() }
  }

  fun moveToPreviousTask() {
    moveToTask(withReady { taskSequenceHandler.getPreviousTask(it.currentTaskId) })
  }

  /** Returns true if the given [taskId] is first task in the sequence of displayed tasks. */
  fun isFirstPosition(taskId: String): Boolean = withReady {
    taskSequenceHandler.isFirstPosition(taskId)
  }

  /** Returns true if the given [taskId] is last task in the sequence of displayed tasks. */
  fun isLastPosition(taskId: String): Boolean = withReady {
    taskSequenceHandler.isLastPosition(taskId)
  }

  /**
   * Returns true if the given [task] with [newValue] would be last in the sequence of displayed
   * tasks. Required for handling conditional tasks, see #2394.
   */
  fun isLastPositionWithValue(task: Task, newValue: TaskData?): Boolean = withReady {
    if (taskDataHandler.getData(task) == newValue) {
      taskSequenceHandler.isLastPosition(task.id)
    } else {
      taskSequenceHandler.checkIfTaskIsLastWithValue(task.id to newValue)
    }
  }

  fun requireSurveyId(): String = withReady { it.surveyId }

  fun isAtFirstTask(): Boolean = withReady {
    return taskSequenceHandler.isFirstPosition(it.currentTaskId)
  }

  /** Retrieves a list of [ValueDelta] for tasks that are part of the current sequence. */
  private fun getDeltas(): List<ValueDelta> {
    val tasksDataMap = taskDataHandler.dataState.value
    val validTasks = taskSequenceHandler.getValidTasks()
    return tasksDataMap
      .filterKeys { it in validTasks }
      .map { (task, taskData) -> ValueDelta(task.id, task.type, taskData) }
  }

  /** Persists the changes locally and enqueues a worker to sync with remote datastore. */
  private fun saveChanges(state: DataCollectionUiState.Ready, deltas: List<ValueDelta>) {
    externalScope.launch(ioDispatcher) {
      val collectionId = offlineUuidGenerator.generateUuid()
      submitDataUseCase.invoke(
        loiId,
        state.job,
        state.surveyId,
        deltas,
        savedStateHandle[TASK_LOI_NAME_KEY],
        collectionId,
      )
    }
  }

  /** Persists the collected data as draft to local storage. */
  private fun saveDraft(taskId: String) {
    val deltas = getDeltas()
    if (uiState.value == DataCollectionUiState.TaskSubmitted || deltas.isEmpty()) return

    viewModelScope.launch(ioDispatcher) {
      val state = uiState.value as? DataCollectionUiState.Ready
      if (state != null) {
        submissionRepository.saveDraftSubmission(
          jobId = jobId,
          loiId = loiId,
          surveyId = state.surveyId,
          deltas = deltas,
          loiName = savedStateHandle[TASK_LOI_NAME_KEY],
          currentTaskId = taskId,
        )
      }
    }
  }

  private fun moveToNextTask() =
    moveToTask(withReady { taskSequenceHandler.getNextTask(it.currentTaskId) })

  private fun moveToTask(taskId: String) = withReady { st ->
    val validIds = taskSequenceHandler.getValidTasks().map { it.id }.toSet()
    val safeId = if (taskId in validIds) taskId else validIds.first()

    savedStateHandle[TASK_POSITION_ID] = safeId
    clearDraft()
    saveDraft(safeId)

    val newPos = taskSequenceHandler.getTaskPosition(safeId)
    _uiState.value = st.copy(currentTaskId = safeId, position = newPos)
  }

  private fun ensureDraftCaches(job: Job) {
    if (!shouldLoadFromDraft || draftCache != null) return

    val serialized: String = savedStateHandle[TASK_DRAFT_VALUES] ?: ""
    if (serialized.isEmpty()) {
      Timber.w("No draft values found; skipping load")
      synchronized(draftLock) {
        if (draftCache == null) {
          draftCache = emptyList()
          draftMapCache = emptyMap()
        }
      }
      return
    }

    val parsed =
      try {
        SubmissionDeltasConverter.fromString(job, serialized)
      } catch (e: Exception) {
        Timber.e(e, "Failed to parse draft submission")
        emptyList()
      }

    synchronized(draftLock) {
      if (draftCache == null) {
        draftCache = parsed
        draftMapCache =
          parsed.associate { (taskId, taskType, value) -> (taskId to taskType) to value }
      }
    }
  }

  private fun resolveInitialTaskId(validIds: List<String>, saved: String?): String =
    saved?.takeIf { it.isNotBlank() && it in validIds } ?: validIds.first()

  private fun getValueFromDraft(job: Job, task: Task): TaskData? {
    if (!shouldLoadFromDraft) return null
    ensureDraftCaches(job)
    val value = draftMapCache?.get(task.id to task.type)
    if (value == null) Timber.w("Value not found for task $task")
    else Timber.d("Value $value found for task $task")
    return value
  }

  private inline fun validateOrShow(taskVm: AbstractTaskViewModel, onValid: () -> Unit) {
    val error = taskVm.validate()
    if (error != null) {
      popups.get().ErrorPopup().show(error)
    } else {
      onValid()
    }
  }

  private inline fun <T> withReadyOrNull(block: (DataCollectionUiState.Ready) -> T): T? {
    val s = uiState.value
    return if (s is DataCollectionUiState.Ready) block(s) else null
  }

  /**
   * Convenience: runs [block] only if the current UiState is Ready. Throws if called while still
   * Loading/Error/Submitted.
   */
  private inline fun <T> withReady(block: (DataCollectionUiState.Ready) -> T): T {
    val s = uiState.value
    check(s is DataCollectionUiState.Ready) { "UI state not Ready, was: $s" }
    return block(s)
  }

  companion object {
    private const val TASK_JOB_ID_KEY = "jobId"
    private const val TASK_LOI_ID_KEY = "locationOfInterestId"
    private const val TASK_LOI_NAME_KEY = "locationOfInterestName"
    private const val TASK_POSITION_ID = "currentTaskId"
    private const val TASK_SHOULD_LOAD_FROM_DRAFT = "shouldLoadFromDraft"
    private const val TASK_DRAFT_VALUES = "draftValues"
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
