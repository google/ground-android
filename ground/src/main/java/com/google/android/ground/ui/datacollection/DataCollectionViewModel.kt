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

import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.android.ground.R
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.domain.usecases.submission.SubmitDataUseCase
import com.google.android.ground.model.Survey
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.Value
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.Task
import com.google.android.ground.repository.LocationOfInterestRepository
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
import com.google.android.ground.ui.datacollection.tasks.point.DropAPinTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.polygon.PolygonDrawingViewModel
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
  private val resources: Resources,
  locationOfInterestRepository: LocationOfInterestRepository,
  surveyRepository: SurveyRepository,
) : AbstractViewModel() {

  private val loiId: String? = savedStateHandle["locationOfInterestId"]
  private val activeSurvey: Survey = requireNotNull(surveyRepository.activeSurvey)
  private val job: Job =
    activeSurvey.getJob(requireNotNull(savedStateHandle["jobId"])) ?: error("empty job")
  val tasks: List<Task> = buildList {
    if (job.suggestLoiTaskType != null && loiId == null) {
      add(createSuggestLoiTask(job.suggestLoiTaskType))
    }
    addAll(job.tasksSorted)
  }

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

  var currentValue: Value? = null

  private val _currentTaskViewModelFlow: StateFlow<AbstractTaskViewModel?> =
    currentPosition
      .combine(taskViewModels) { position, viewModels -> viewModels[position] }
      .stateIn(viewModelScope, SharingStarted.Lazily, null)

  @OptIn(ExperimentalCoroutinesApi::class)
  val currentValueFlow: Flow<Value?> =
    _currentTaskViewModelFlow.flatMapLatest { it?.value ?: flowOf(null) }

  lateinit var submissionId: String

  fun getTaskViewModel(position: Int): AbstractTaskViewModel {
    val viewModels = taskViewModels.value

    val task = tasks[position]
    if (position < viewModels.size) {
      return viewModels[position]
    }
    val viewModel = viewModelFactory.create(getViewModelClass(task.type))
    // TODO(#1146): Pass in the existing value if there is one.
    viewModel.initialize(job, task, null)
    addTaskViewModel(viewModel)
    return viewModel
  }

  private fun addTaskViewModel(taskViewModel: AbstractTaskViewModel) {
    taskViewModels.value.add(taskViewModel)
    taskViewModels.value = taskViewModels.value
  }

  /**
   * Validates the user's input and displays an error if the user input was invalid. Moves back to
   * the previous Data Collection screen if the user input was valid.
   */
  fun onPreviousClicked(position: Int, taskViewModel: AbstractTaskViewModel) {
    check(position != 0)

    val validationError = taskViewModel.validate()
    if (validationError != null) {
      popups.get().showError(validationError)
      return
    }

    updateCurrentPosition(position - 1)
  }

  /**
   * Validates the user's input and displays an error if the user input was invalid. Progresses to
   * the next Data Collection screen if the user input was valid.
   */
  fun onNextClicked(position: Int, taskViewModel: AbstractTaskViewModel) {
    val validationError = taskViewModel.validate()
    if (validationError != null) {
      popups.get().showError(validationError)
      return
    }

    data[taskViewModel.task] = currentValue

    if (!isLastPosition(position)) {
      updateCurrentPosition(position + 1)
    } else {
      val deltas = data.map { (task, value) -> ValueDelta(task.id, task.type, value) }
      saveChanges(deltas)

      // Move to home screen and display a confirmation dialog after that.
      navigator.navigate(HomeScreenFragmentDirections.showHomeScreen())
      navigator.navigate(
        DataSubmissionConfirmationDialogFragmentDirections
          .showSubmissionConfirmationDialogFragment()
      )
    }
  }

  /** Persists the changes locally and enqueues a worker to sync with remote datastore. */
  private fun saveChanges(deltas: List<ValueDelta>) {
    externalScope.launch(ioDispatcher) { submitDataUseCase.invoke(loiId, job, surveyId, deltas) }
  }

  /** Returns the position of the task fragment visible to the user. */
  fun getVisibleTaskPosition() = currentPosition.value

  /** Displays the task at the given position to the user. */
  fun updateCurrentPosition(position: Int) {
    savedStateHandle[TASK_POSITION_KEY] = position
  }

  /** Returns true if the given task position is last. */
  fun isLastPosition(taskPosition: Int): Boolean {
    val finalTaskPosition = tasks.size - 1

    assert(finalTaskPosition >= 0)
    assert(taskPosition in 0..finalTaskPosition)

    return taskPosition == finalTaskPosition
  }

  private fun createSuggestLoiTask(taskType: Task.Type): Task =
    Task(id = "-1", index = -1, taskType, resources.getString(R.string.new_site), isRequired = true)

  companion object {
    private const val TASK_POSITION_KEY = "currentPosition"

    fun getViewModelClass(taskType: Task.Type): Class<out AbstractTaskViewModel> =
      when (taskType) {
        Task.Type.TEXT -> TextTaskViewModel::class.java
        Task.Type.MULTIPLE_CHOICE -> MultipleChoiceTaskViewModel::class.java
        Task.Type.PHOTO -> PhotoTaskViewModel::class.java
        Task.Type.NUMBER -> NumberTaskViewModel::class.java
        Task.Type.DATE -> DateTaskViewModel::class.java
        Task.Type.TIME -> TimeTaskViewModel::class.java
        Task.Type.DROP_A_PIN -> DropAPinTaskViewModel::class.java
        Task.Type.DRAW_POLYGON -> PolygonDrawingViewModel::class.java
        Task.Type.CAPTURE_LOCATION -> CaptureLocationTaskViewModel::class.java
        Task.Type.UNKNOWN -> throw IllegalArgumentException("Unsupported task type: $taskType")
      }
  }
}
