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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.domain.usecases.submission.SubmitDataUseCase
import com.google.android.ground.model.Survey
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.Value
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.room.converter.SubmissionDeltasConverter
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.LocationOfInterestHelper
import com.google.android.ground.ui.common.Navigator
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
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Provider
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
  private val navigator: Navigator,
  private val submitDataUseCase: SubmitDataUseCase,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val savedStateHandle: SavedStateHandle,
  private val submissionRepository: SubmissionRepository,
  locationOfInterestRepository: LocationOfInterestRepository,
  surveyRepository: SurveyRepository,
) : AbstractViewModel() {

  private val jobId: String = requireNotNull(savedStateHandle[TASK_JOB_ID_KEY])
  private val loiId: String? = savedStateHandle[TASK_LOI_ID_KEY]
  /** True iff the user is expected to produce a new LOI in the current data collection flow. */
  private val isAddLoiFlow = loiId == null

  private var shouldLoadFromDraft: Boolean = savedStateHandle[TASK_SHOULD_LOAD_FROM_DRAFT] ?: false
  private var draftDeltas: List<ValueDelta>? = null

  private val activeSurvey: Survey = requireNotNull(surveyRepository.activeSurvey)
  private val job: Job = activeSurvey.getJob(jobId) ?: error("couldn't retrieve job for $jobId")
  // LOI creation task is included only on "new data collection site" flow..
  val tasks: List<Task> =
    if (isAddLoiFlow) job.tasksSorted else job.tasksSorted.filterNot { it.isAddLoiTask }

  val surveyId: String = surveyRepository.lastActiveSurveyId

  val jobName: StateFlow<String> =
    MutableStateFlow(job.name ?: "").stateIn(viewModelScope, SharingStarted.Lazily, "")
  val loiName: StateFlow<String> =
    (if (loiId == null) flowOf("")
      else
        flow {
          val loi = locationOfInterestRepository.getOfflineLoi(surveyId, loiId)
          val label = locationOfInterestHelper.getLabel(loi)
          emit(label)
        })
      .stateIn(viewModelScope, SharingStarted.Lazily, "")

  private val taskViewModels: MutableStateFlow<MutableList<AbstractTaskViewModel>> =
    MutableStateFlow(mutableListOf())

  private val data: MutableMap<Task, Value?> = LinkedHashMap()

  // Tracks the task's current position in the list of tasks for the current job
  var currentPosition: StateFlow<Int> = savedStateHandle.getStateFlow(TASK_POSITION_KEY, 0)

  lateinit var submissionId: String

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

  private fun getValueFromDraft(task: Task): Value? {
    for ((taskId, taskType, value) in getDraftDeltas()) {
      if (taskId == task.id && taskType == task.type) {
        Timber.d("Value $value found for task $task")
        return value
      }
    }
    Timber.w("Value not found for task $task")
    return null
  }

  fun getTaskViewModel(position: Int): AbstractTaskViewModel? {
    val viewModels = taskViewModels.value

    val task = tasks[position]
    if (position < viewModels.size) {
      return viewModels[position]
    }
    return try {
      val viewModel = viewModelFactory.create(getViewModelClass(task.type))
      val value: Value? = if (shouldLoadFromDraft) getValueFromDraft(task) else null
      viewModel.initialize(job, task, value)
      addTaskViewModel(viewModel)
      viewModel
    } catch (e: Exception) {
      Timber.e("ignoring task with invalid type: $task.type")
      null
    }
  }

  private fun addTaskViewModel(taskViewModel: AbstractTaskViewModel) {
    taskViewModels.value.add(taskViewModel)
    taskViewModels.value = taskViewModels.value
  }

  /**
   * Validates the user's input and displays an error if the user input was invalid. Moves back to
   * the previous Data Collection screen if the user input was valid.
   */
  suspend fun onPreviousClicked(position: Int, taskViewModel: AbstractTaskViewModel) {
    check(position != 0)

    val validationError = taskViewModel.validate()
    if (validationError != null) {
      popups.get().showError(validationError)
      return
    }

    data[taskViewModel.task] = taskViewModel.taskValue.firstOrNull()

    updateCurrentPosition(position - 1)
  }

  /**
   * Validates the user's input and displays an error if the user input was invalid. Progresses to
   * the next Data Collection screen if the user input was valid.
   */
  suspend fun onNextClicked(position: Int, taskViewModel: AbstractTaskViewModel) {
    val validationError = taskViewModel.validate()
    if (validationError != null) {
      popups.get().showError(validationError)
      return
    }

    data[taskViewModel.task] = taskViewModel.taskValue.firstOrNull()

    if (!isLastPosition(position)) {
      updateCurrentPosition(position + 1)
    } else {
      clearDraft()
      saveChanges(getDeltas())

      // Move to home screen and display a confirmation dialog after that.
      navigator.navigate(HomeScreenFragmentDirections.showHomeScreen())
      navigator.navigate(
        DataSubmissionConfirmationDialogFragmentDirections
          .showSubmissionConfirmationDialogFragment()
      )
    }
  }

  private fun getDeltas(): List<ValueDelta> =
    data.map { (task, value) -> ValueDelta(task.id, task.type, value) }

  /** Persists the changes locally and enqueues a worker to sync with remote datastore. */
  private fun saveChanges(deltas: List<ValueDelta>) {
    externalScope.launch(ioDispatcher) { submitDataUseCase.invoke(loiId, job, surveyId, deltas) }
  }

  /** Persists the collected data as draft to local storage. */
  private fun saveDraft() {
    externalScope.launch(ioDispatcher) {
      submissionRepository.saveDraftSubmission(
        jobId = jobId,
        loiId = loiId,
        surveyId = surveyId,
        deltas = getDeltas(),
      )
    }
  }

  /** Clears all persisted drafts from local storage. */
  fun clearDraft() {
    externalScope.launch(ioDispatcher) { submissionRepository.deleteDraftSubmission() }
  }

  /** Returns the position of the task fragment visible to the user. */
  fun getVisibleTaskPosition() = currentPosition.value

  /** Displays the task at the given position to the user. */
  fun updateCurrentPosition(position: Int) {
    savedStateHandle[TASK_POSITION_KEY] = position

    // Save collected data as draft
    clearDraft()
    saveDraft()
  }

  /** Returns true if the given task position is last. */
  fun isLastPosition(taskPosition: Int): Boolean {
    val finalTaskPosition = tasks.size - 1

    assert(finalTaskPosition >= 0)
    assert(taskPosition in 0..finalTaskPosition)

    return taskPosition == finalTaskPosition
  }

  companion object {
    private const val TASK_JOB_ID_KEY = "jobId"
    private const val TASK_LOI_ID_KEY = "locationOfInterestId"
    private const val TASK_POSITION_KEY = "currentPosition"
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
