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

import androidx.lifecycle.*
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.Survey
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.model.task.Task
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.*
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.date.DateTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.number.NumberTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.photo.PhotoTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.point.DropAPinTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.polygon.PolygonDrawingViewModel
import com.google.android.ground.ui.datacollection.tasks.text.TextTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.time.TimeTaskViewModel
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.util.combineWith
import dagger.hilt.android.lifecycle.HiltViewModel
import java8.util.Optional
import javax.inject.Inject
import javax.inject.Provider
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

/** View model for the Data Collection fragment. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DataCollectionViewModel
@Inject
internal constructor(
  private val viewModelFactory: ViewModelFactory,
  private val submissionRepository: SubmissionRepository,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val locationOfInterestHelper: LocationOfInterestHelper,
  private val popups: Provider<EphemeralPopups>,
  private val navigator: Navigator,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val savedStateHandle: SavedStateHandle,
  surveyRepository: SurveyRepository,
) : AbstractViewModel() {

  // TODO(#1541): Set loiId once Suggest LOI task is completed.
  private val loiId: StateFlow<String?> = MutableStateFlow(savedStateHandle["locationOfInterestId"])
  private val activeSurvey: Survey = requireNotNull(surveyRepository.activeSurvey)
  private val job: Job =
    activeSurvey.getJob(requireNotNull(savedStateHandle["jobId"])).orElseThrow()

  val surveyId: String = surveyRepository.lastActiveSurveyId
  val submission: StateFlow<Submission?> =
    loiId
      .flatMapLatest {
        if (it == null) flowOf(null)
        else submissionRepository.createSubmission(surveyId, it).toFlowable().asFlow()
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, null)

  val jobName: StateFlow<String> =
    MutableStateFlow(job.name ?: "").stateIn(viewModelScope, SharingStarted.Lazily, "")
  val loiName: StateFlow<String> =
    loiId
      .flatMapLatest { id ->
        if (id == null) flowOf("")
        else
          locationOfInterestRepository
            .getOfflineLocationOfInterest(surveyId, id)
            .toFlowable()
            .asFlow()
            .map { locationOfInterestHelper.getLabel(it) }
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, "")

  private val taskViewModels:
    @Hot(replays = true)
    MutableLiveData<MutableList<AbstractTaskViewModel>> =
    MutableLiveData(mutableListOf())

  private val responses: MutableMap<Task, TaskData?> = HashMap()

  private val currentPositionKey = "currentPosition"
  // Tracks the user's current position in the list of tasks for the current Job
  var currentPosition: @Hot(replays = true) MutableLiveData<Int> =
    savedStateHandle.getLiveData(currentPositionKey, 0)

  var currentTaskData: TaskData? = null

  private var currentTaskViewModel: AbstractTaskViewModel? = null

  private val currentTaskViewModelLiveData =
    currentPosition.combineWith(taskViewModels) { position, viewModels ->
      if (position!! < viewModels!!.size) {
        currentTaskViewModel = viewModels[position]
      }

      currentTaskViewModel
    }

  val currentTaskDataLiveData =
    Transformations.switchMap(currentTaskViewModelLiveData) { it?.taskData }

  lateinit var submissionId: String

  fun getTaskViewModel(position: Int): AbstractTaskViewModel {
    val viewModels = taskViewModels.value
    requireNotNull(viewModels)
    // TODO(#1541): Insert Suggest LOI task to taskList. This will probably involve creating a
    //  separate ViewModel field for storing the tasks rather than getting them directly from the
    //  job.
    val tasks = job.tasksSorted

    val task = tasks[position]
    if (position < viewModels.size) {
      return viewModels[position]
    }
    val viewModel = viewModelFactory.create(getViewModelClass(task.type))
    // TODO(#1146): Pass in the existing taskData if there is one
    viewModel.initialize(task, Optional.empty())
    addTaskViewModel(viewModel)
    return viewModel
  }

  private fun addTaskViewModel(taskViewModel: AbstractTaskViewModel) {
    taskViewModels.value?.add(taskViewModel)
    taskViewModels.value = taskViewModels.value
  }

  /**
   * Validates the user's input and displays an error if the user input was invalid. Progresses to
   * the next Data Collection screen if the user input was valid.
   */
  fun onContinueClicked() {
    val currentTask = currentTaskViewModel ?: return

    val validationError = currentTask.validate()
    if (validationError != null) {
      popups.get().showError(validationError)
      return
    }

    responses[currentTask.task] = currentTaskData

    val currentTaskPosition = currentPosition.value!!
    val finalTaskPosition = job.tasks.size - 1

    assert(finalTaskPosition >= 0)
    assert(currentTaskPosition in 0..finalTaskPosition)

    if (currentTaskPosition != finalTaskPosition) {
      setCurrentPosition(currentPosition.value!! + 1)
    } else {
      val taskDataDeltas =
        responses.map { (task, taskData) ->
          TaskDataDelta(task.id, task.type, Optional.ofNullable(taskData))
        }
      val submission = submission.value!!
      saveChanges(submission, taskDataDeltas)

      // Move to home screen and display a confirmation dialog after that.
      navigator.navigate(HomeScreenFragmentDirections.showHomeScreen())
      navigator.navigate(
        DataSubmissionConfirmationDialogFragmentDirections
          .showSubmissionConfirmationDialogFragment()
      )
    }
  }

  /** Persists the changes locally and enqueues a worker to sync with remote datastore. */
  private fun saveChanges(submission: Submission, taskDataDeltas: List<TaskDataDelta>) {
    externalScope.launch(ioDispatcher) {
      //      submissionRepository
      //        .createOrUpdateSubmission(submission, taskDataDeltas, isNew = true)
      //        .blockingAwait()
    }
  }

  fun setCurrentPosition(position: Int) {
    savedStateHandle[currentPositionKey] = position
  }

  companion object {
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
        Task.Type.UNKNOWN -> throw IllegalArgumentException("Unsupported task type: $taskType")
      }
  }
}
