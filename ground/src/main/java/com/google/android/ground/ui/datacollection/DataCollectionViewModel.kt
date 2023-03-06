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
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.model.task.Task
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.*
import com.google.android.ground.ui.editsubmission.AbstractTaskViewModel
import com.google.android.ground.ui.editsubmission.TaskViewFactory
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.util.combineWith
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import java8.util.Optional
import javax.inject.Inject
import javax.inject.Provider
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** View model for the Data Collection fragment. */
@HiltViewModel
class DataCollectionViewModel
@Inject
internal constructor(
  private val viewModelFactory: ViewModelFactory,
  private val submissionRepository: SubmissionRepository,
  private val locationOfInterestHelper: LocationOfInterestHelper,
  private val popups: Provider<EphemeralPopups>,
  private val navigator: Navigator,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val savedStateHandle: SavedStateHandle
) : AbstractViewModel() {

  val submission: @Hot(replays = true) LiveData<Submission>
  val jobName: @Hot(replays = true) LiveData<String>
  val loiName: @Hot(replays = true) LiveData<String>

  private val taskViewModels:
    @Hot(replays = true)
    MutableLiveData<MutableList<AbstractTaskViewModel>> =
    MutableLiveData(mutableListOf())
  private val argsProcessor: @Hot(replays = true) FlowableProcessor<DataCollectionFragmentArgs> =
    BehaviorProcessor.create()

  private val responses: MutableMap<Task, TaskData?> = HashMap()

  private val currentPositionKey = "currentPosition"
  // Tracks the user's current position in the list of tasks for the current Job
  var currentPosition: @Hot(replays = true) MutableLiveData<Int> =
    savedStateHandle.getLiveData(currentPositionKey, 0)

  var currentTaskData: TaskData? = null

  var currentTaskViewModel: AbstractTaskViewModel? = null

  private val currentTaskViewModelLiveData =
    currentPosition.combineWith(taskViewModels) { position, viewModels ->
      if (position!! < viewModels!!.size) {
        currentTaskViewModel = viewModels[position]
      }

      currentTaskViewModel
    }

  val currentTaskDataLiveData =
    Transformations.switchMap(currentTaskViewModelLiveData) { it?.taskData }

  lateinit var surveyId: String
  lateinit var submissionId: String

  init {
    val submissionStream: Flowable<Submission> =
      argsProcessor.switchMapSingle { args ->
        surveyId = args.surveyId

        submissionRepository
          .createSubmission(
            args.surveyId,
            args.locationOfInterestId,
          )
          .doOnSuccess { submissionId = it.id }
      }

    submission = LiveDataReactiveStreams.fromPublisher(submissionStream)

    jobName =
      LiveDataReactiveStreams.fromPublisher(submissionStream.map { it.locationOfInterest.job.name })

    loiName =
      LiveDataReactiveStreams.fromPublisher(
        submissionStream.map {
          locationOfInterestHelper.getLabel(Optional.of(it.locationOfInterest))
        }
      )
  }

  fun loadSubmissionDetails(args: DataCollectionFragmentArgs) = argsProcessor.onNext(args)

  fun getTaskViewModel(position: Int): AbstractTaskViewModel {
    val viewModels = taskViewModels.value
    requireNotNull(viewModels)
    // TODO(#1146): Show toast or error if submission is null
    val tasks = requireNotNull(submission.value).job.tasksSorted

    val task = tasks[position]
    if (position < viewModels.size) {
      return viewModels[position]
    }
    val viewModel = viewModelFactory.create(TaskViewFactory.getViewModelClass(task.type))
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

    val submission = submission.value!!
    val currentTaskPosition = currentPosition.value!!
    val finalTaskPosition = submission.job.tasks.size - 1

    assert(finalTaskPosition >= 0)
    assert(currentTaskPosition in 0..finalTaskPosition)

    if (currentTaskPosition != finalTaskPosition) {
      setCurrentPosition(currentPosition.value!! + 1)
    } else {
      val taskDataDeltas =
        responses.map { (task, taskData) ->
          TaskDataDelta(task.id, task.type, Optional.ofNullable(taskData))
        }
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
      submissionRepository
        .createOrUpdateSubmission(submission, taskDataDeltas, isNew = true)
        .blockingAwait()
    }
  }

  fun setCurrentPosition(position: Int) {
    savedStateHandle[currentPositionKey] = position
  }
}
