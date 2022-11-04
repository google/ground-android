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

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.model.task.Task
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.LocationOfInterestHelper
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.editsubmission.AbstractTaskViewModel
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.util.combineWith
import com.google.common.collect.ImmutableList
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import java8.util.Optional
import javax.inject.Inject
import javax.inject.Provider

/** View model for the Data Collection fragment. */
class DataCollectionViewModel
@Inject
internal constructor(
  private val submissionRepository: SubmissionRepository,
  private val locationOfInterestHelper: LocationOfInterestHelper,
  private val popups: Provider<EphemeralPopups>,
  private val navigator: Navigator
) : AbstractViewModel() {
  val submission: @Hot(replays = true) LiveData<Loadable<Submission>>
  val jobName: @Hot(replays = true) LiveData<String>
  val loiName: @Hot(replays = true) LiveData<String>

  private val taskViewModels:
    @Hot(replays = true)
    MutableLiveData<MutableList<AbstractTaskViewModel>> =
    MutableLiveData(mutableListOf())
  private val argsProcessor: @Hot(replays = true) FlowableProcessor<DataCollectionFragmentArgs> =
    BehaviorProcessor.create()

  private val responses: MutableMap<Task, TaskData?> = HashMap()

  // Tracks the user's current position in the list of tasks for the current Job
  var currentPosition: @Hot(replays = true) MutableLiveData<Int> = MutableLiveData(0)

  var currentTaskData: TaskData? = null

  var currentTaskViewModel: AbstractTaskViewModel? = null

  val currentTaskViewModelLiveData =
    currentPosition.combineWith(taskViewModels) { position, viewModels ->
      if (position!! < viewModels!!.size) {
        currentTaskViewModel = viewModels[position]
      }

      currentTaskViewModel
    }

  val currentTaskDataLiveData =
    Transformations.switchMap(currentTaskViewModelLiveData) { it?.taskData }

  init {
    val submissionStream: Flowable<Loadable<Submission>> =
      argsProcessor.switchMapSingle { args ->
        submissionRepository
          .createSubmission(args.surveyId, args.locationOfInterestId, args.submissionId)
          .map { Loadable.loaded(it) }
          .onErrorReturn { Loadable.error(it) }
      }

    submission = LiveDataReactiveStreams.fromPublisher(submissionStream)

    jobName =
      LiveDataReactiveStreams.fromPublisher(
        submissionStream.map { submission ->
          submission.value().map { it.locationOfInterest.job.name }.orElse("")
        }
      )

    loiName =
      LiveDataReactiveStreams.fromPublisher(
        submissionStream
          .map { submission -> submission.value().map { it.locationOfInterest } }
          .map { locationOfInterest -> locationOfInterestHelper.getLabel(locationOfInterest) }
      )
  }

  fun loadSubmissionDetails(args: DataCollectionFragmentArgs) = argsProcessor.onNext(args)

  fun addTaskViewModel(taskViewModel: AbstractTaskViewModel) {
    taskViewModels.value?.add(taskViewModel)
    taskViewModels.value = taskViewModels.value
  }

  /**
   * Validates the user's input and returns an error string if the user input was invalid.
   * Progresses to the next Data Collection screen if the user input was valid.
   */
  fun onContinueClicked(): Single<String> {
    val currentTask = currentTaskViewModel ?: return Single.never()
    val validationError = currentTask.validate()
    if (validationError == null) {
      responses[currentTask.task] = currentTaskData
      val finalTaskPosition = submission.value!!.value().map { it.job.tasks.size }.orElse(0) - 1

      if (currentPosition.value!! == finalTaskPosition) {
        submission.value!!.value().ifPresent {
          val taskDataDeltas = ImmutableList.builder<TaskDataDelta>()

          responses.forEach { (task, taskData) ->
            taskDataDeltas.add(TaskDataDelta(task.id, task.type, Optional.ofNullable(taskData)))
          }
          submissionRepository.createOrUpdateSubmission(it, taskDataDeltas.build(), isNew = true)
        }

        navigator.navigate(HomeScreenFragmentDirections.showHomeScreen())
        return Single.never()
      }

      currentPosition.postValue(currentPosition.value!! + 1)

      return Single.never()
    } else {
      popups.get().showError(validationError)
    }

    return Single.just(validationError)
  }
}
