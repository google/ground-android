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

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
  private val viewModelFactory: ViewModelFactory,
  private val locationOfInterestHelper: LocationOfInterestHelper,
  private val popups: Provider<EphemeralPopups>,
  private val submitDataUseCase: SubmitDataUseCase,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val submissionRepository: SubmissionRepository,
  private val offlineUuidGenerator: OfflineUuidGenerator,
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

  val loiNameDialogOpen = mutableStateOf(false)

  private val taskViewModels = MutableStateFlow(mutableMapOf<String, AbstractTaskViewModel>())
  private val taskDataHandler = TaskDataHandler()
  private lateinit var taskSequenceHandler: TaskSequenceHandler

  @Volatile private var draftCache: List<ValueDelta>? = null
  @Volatile private var draftMapCache: Map<Pair<String, Task.Type>, TaskData?>? = null
  @Volatile private var draftsEnabled = true
  private val draftLock = Any()

  fun initialize(savedStateHandle: SavedStateHandle, scope: CoroutineScope) {
    this.savedStateHandle = savedStateHandle
    this.scope = scope

    jobId = requireNotNull(savedStateHandle[TASK_JOB_ID_KEY])
    loiId = savedStateHandle[TASK_LOI_ID_KEY]
    shouldLoadFromDraft = savedStateHandle[TASK_SHOULD_LOAD_FROM_DRAFT] ?: false

    scope.launch { initUiState() }
  }

  private suspend fun initUiState() {
    _uiState.value = computeInitialUiState()
  }

  private suspend fun computeInitialUiState(): DataCollectionUiState {
    var errorMessage: String? = null

    // 1) Load survey with timeout
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
        if (loiId == null) job.tasksSorted else job.tasksSorted.filterNot { it.isAddLoiTask }
      } else {
        emptyList()
      }

    // 4) Sequence + validation
    if (job != null) {
      taskSequenceHandler = TaskSequenceHandler(tasks, taskDataHandler)
      val valid = taskSequenceHandler.getValidTasks()
      if (valid.isEmpty()) errorMessage = "No valid tasks for this job."
    }

    // 5) Error short-circuit
    errorMessage?.let {
      return DataCollectionUiState.Error(it)
    }

    // 6) Happy path
    val valid = taskSequenceHandler.getValidTasks()
    val initialTaskId =
      resolveInitialTaskId(
        validIds = valid.map { it.id },
        saved = savedStateHandle[TASK_POSITION_ID],
      )
    savedStateHandle[TASK_POSITION_ID] = initialTaskId

    val loiName =
      when {
        loiId == null -> {
          // user is adding a new LOI, so we may rely on savedStateHandle value
          savedStateHandle[TASK_LOI_NAME_KEY] ?: ""
        }
        else -> {
          // look up the LOI in repository
          withContext(ioDispatcher) {
            locationOfInterestRepository.getOfflineLoi(survey!!.id, loiId!!)?.let {
              locationOfInterestHelper.getDisplayLoiName(it)
            }
          }
        }
      }

    return DataCollectionUiState.Ready(
      surveyId = requireNotNull(survey!!.id),
      job = job!!,
      loiName = loiName ?: "",
      tasks = tasks,
      isAddLoiFlow = (loiId == null),
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
      val taskData = if (shouldLoadFromDraft) getValueFromDraft(state.job, task) else null
      created.initialize(state.job, task, taskData)
      taskDataHandler.setData(task, taskData)
      taskViewModels.value[task.id] = created
    }
    viewModel
  }

  fun onPreviousClicked(taskViewModel: AbstractTaskViewModel) = withReady { _ ->
    val task = taskViewModel.task
    val taskValue = taskViewModel.taskTaskData.value

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

  fun saveCurrentState() {
    if (!draftsEnabled) return
    withReadyOrNull { state ->
      val taskId = state.currentTaskId
      getTaskViewModel(taskId)?.let { vm ->
        taskDataHandler.setData(vm.task, vm.taskTaskData.value)
        savedStateHandle[TASK_POSITION_ID] = taskId
        saveDraft(taskId)
      }
    }
  }

  fun clearDraftBlocking() {
    suppressDrafts()
    clearDraft()
  }

  fun moveToPreviousTask() {
    moveToTask(withReady { taskSequenceHandler.getPreviousTask(it.currentTaskId) })
  }

  fun moveToNextTask() {
    moveToTask(withReady { taskSequenceHandler.getNextTask(it.currentTaskId) })
  }

  fun isFirstPosition(taskId: String): Boolean = withReady {
    taskSequenceHandler.isFirstPosition(taskId)
  }

  fun isLastPosition(taskId: String): Boolean = withReady {
    taskSequenceHandler.isLastPosition(taskId)
  }

  fun isLastPositionWithValue(task: Task, newValue: TaskData?): Boolean = withReady {
    if (taskDataHandler.getData(task) == newValue) {
      taskSequenceHandler.isLastPosition(task.id)
    } else {
      taskSequenceHandler.checkIfTaskIsLastWithValue(task.id to newValue)
    }
  }

  fun requireSurveyId(): String = withReady { it.surveyId }

  fun isAtFirstTask(): Boolean = withReady { taskSequenceHandler.isFirstPosition(it.currentTaskId) }

  private fun moveToTask(taskId: String) = withReady { st ->
    val validIds = taskSequenceHandler.getValidTasks().map { it.id }.toSet()
    val safeId = if (taskId in validIds) taskId else validIds.first()

    savedStateHandle[TASK_POSITION_ID] = safeId
    saveDraft(safeId)

    val newPos = taskSequenceHandler.getTaskPosition(safeId)
    _uiState.value = st.copy(currentTaskId = safeId, position = newPos)
  }

  private fun getDeltas(): List<ValueDelta> {
    val tasksDataMap = taskDataHandler.dataState.value
    val validTasks = taskSequenceHandler.getValidTasks()
    return tasksDataMap
      .filterKeys { it in validTasks }
      .map { (task, taskData) -> ValueDelta(task.id, task.type, taskData) }
  }

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

  private fun clearDraft() {
    scope.launch(ioDispatcher) { submissionRepository.deleteDraftSubmission() }
  }

  private fun saveDraft(taskId: String) {
    val deltas = getDeltas()
    if (uiState.value == DataCollectionUiState.TaskSubmitted || deltas.isEmpty()) return

    scope.launch(ioDispatcher) {
      val state = uiState.value as? DataCollectionUiState.Ready ?: return@launch
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

  private fun suppressDrafts() {
    draftsEnabled = false
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
