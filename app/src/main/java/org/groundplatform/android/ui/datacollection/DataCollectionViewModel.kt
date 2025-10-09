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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.groundplatform.android.coroutines.ApplicationScope
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.data.local.room.converter.SubmissionDeltasConverter
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.ValueDelta
import org.groundplatform.android.model.submission.isNotNullOrEmpty
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.repository.SubmissionRepository
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.ui.common.EphemeralPopups
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
  private val savedStateHandle: SavedStateHandle,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val submissionRepository: SubmissionRepository,
  private val submitDataUseCase: SubmitDataUseCase,
  private val offlineUuidGenerator: OfflineUuidGenerator,
  private val popups: Provider<EphemeralPopups>,
  private val viewModelFactory: ViewModelFactory,
  private val dataCollectionInitializer: DataCollectionInitializer,
) : AbstractViewModel() {

  private val _uiState = MutableStateFlow<DataCollectionUiState>(DataCollectionUiState.Loading)
  val uiState: StateFlow<DataCollectionUiState> = _uiState

  val loiNameDialogOpen = mutableStateOf(false)
  private var shouldLoadFromDraft: Boolean = savedStateHandle[TASK_SHOULD_LOAD_FROM_DRAFT] ?: false

  private val jobId: String = requireNotNull(savedStateHandle[TASK_JOB_ID_KEY])
  private val loiId: String? = savedStateHandle[TASK_LOI_ID_KEY]
  private val loiName: String? = savedStateHandle[TASK_LOI_NAME_KEY]

  private val taskDataHandler = TaskDataHandler()
  private lateinit var taskSequenceHandler: TaskSequenceHandler
  private val taskViewModels = MutableStateFlow(mutableMapOf<String, AbstractTaskViewModel>())

  private val draftLock = Any()
  @Volatile private var draftCache: List<ValueDelta>? = null
  @Volatile private var draftMapCache: Map<Pair<String, Task.Type>, TaskData?>? = null
  @Volatile private var draftsEnabled = true

  init {
    viewModelScope.launch {
      val initResult = dataCollectionInitializer.initialize(savedStateHandle, jobId, loiId, loiName)

      if (initResult is DataCollectionUiState.Ready) {
        taskSequenceHandler = TaskSequenceHandler(initResult.tasks, taskDataHandler)
      }

      if (initResult is DataCollectionUiState.Error) {
        Timber.e(initResult.cause, "Initialization failed code=%s", initResult.code)
      }
      _uiState.value = initResult
    }
  }

  fun setLoiName(name: String) {
    savedStateHandle[TASK_LOI_NAME_KEY] = name
    _uiState.update { state ->
      (state as? DataCollectionUiState.Ready)?.copy(loiName = name) ?: state
    }
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

  fun isAtFirstTask(): Boolean = withReady { taskSequenceHandler.isFirstPosition(it.currentTaskId) }

  fun clearDraftBlocking() {
    suppressDrafts()
    clearDraft()
  }

  fun requireSurveyId(): String = withReady { it.surveyId }

  fun saveCurrentState() {
    if (!isReady() || !draftsEnabled) return

    withReadyOrNull { state ->
      val taskId = state.currentTaskId
      getTaskViewModel(taskId)?.let { vm ->
        taskDataHandler.setData(vm.task, vm.taskTaskData.value)
        savedStateHandle[TASK_POSITION_ID] = taskId
        saveDraft(taskId)
      }
    }
  }

  fun moveToPreviousTask() {
    moveToTask(withReady { taskSequenceHandler.getPreviousTask(it.currentTaskId) })
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

  fun getTypedLoiNameOrEmpty(): String = savedStateHandle.get<String>(TASK_LOI_NAME_KEY).orEmpty()

  fun isReady(): Boolean = uiState.value is DataCollectionUiState.Ready

  private fun moveToNextTask() {
    moveToTask(withReady { taskSequenceHandler.getNextTask(it.currentTaskId) })
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

  private fun suppressDrafts() {
    draftsEnabled = false
  }

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

  private fun saveDraft(taskId: String) {
    val deltas = getDeltas()
    val state = uiState.value

    if (
      state == DataCollectionUiState.TaskSubmitted ||
        deltas.isEmpty() ||
        state !is DataCollectionUiState.Ready
    ) {
      return
    }

    viewModelScope.launch(ioDispatcher) {
      submissionRepository.saveDraftSubmission(
        jobId = jobId,
        loiId = loiId,
        surveyId = state.surveyId,
        deltas = deltas,
        loiName = savedStateHandle.get<String>(TASK_LOI_NAME_KEY),
        currentTaskId = taskId,
      )
    }
  }

  private fun clearDraft() {
    externalScope.launch(ioDispatcher) { submissionRepository.deleteDraftSubmission() }
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

  companion object {
    private const val TASK_JOB_ID_KEY = "jobId"
    private const val TASK_LOI_ID_KEY = "locationOfInterestId"
    private const val TASK_LOI_NAME_KEY = "locationOfInterestName"
    private const val TASK_POSITION_ID = "currentTaskId"
    private const val TASK_DRAFT_VALUES = "draftValues"
    private const val TASK_SHOULD_LOAD_FROM_DRAFT = "shouldLoadFromDraft"

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
