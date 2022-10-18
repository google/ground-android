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
import com.google.android.ground.model.submission.Response
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.LocationOfInterestHelper
import com.google.android.ground.ui.editsubmission.AbstractTaskViewModel
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject
import javax.inject.Provider

/** View model for the Data Collection fragment. */
class DataCollectionViewModel
@Inject
internal constructor(
  private val submissionRepository: SubmissionRepository,
  private val locationOfInterestHelper: LocationOfInterestHelper,
  private val popups: Provider<EphemeralPopups>,
) : AbstractViewModel() {
  val submission: @Hot(replays = true) LiveData<Loadable<Submission>>
  val jobName: @Hot(replays = true) LiveData<String>
  val loiName: @Hot(replays = true) LiveData<String>
  /** "Continue" button clicks. */
  private val continueClicks: @Hot PublishProcessor<Nil> = PublishProcessor.create()
  /** Outcome of user clicking "Save". */
  val continueResults: Observable<String?>

  private val taskViewModels:
    @Hot(replays = true)
    MutableLiveData<MutableList<AbstractTaskViewModel>> =
    MutableLiveData(mutableListOf())
  private val argsProcessor: @Hot(replays = true) FlowableProcessor<DataCollectionFragmentArgs> =
    BehaviorProcessor.create()

  private val responses: MutableMap<String, Response?> = HashMap()

  // Tracks the user's current position in the list of tasks for the current Job
  val currentPosition: MutableLiveData<Int> = MutableLiveData(0)

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

    continueResults = continueClicks.toObservable().switchMapSingle { onContinueClicked() }
  }

  fun loadSubmissionDetails(args: DataCollectionFragmentArgs) = argsProcessor.onNext(args)

  fun addTaskViewModel(taskViewModel: AbstractTaskViewModel) {
    taskViewModels.value!!.add(taskViewModel)
  }

  /**
   * Validates the user's input and returns an error string if the user input was invalid.
   * Progresses to the next Data Collection screen if the user input was valid.
   */
  fun onContinueClicked(): Single<String> {
    val currentTask = taskViewModels.value!![currentPosition.value ?: 0]
    currentPosition.postValue((currentPosition.value ?: 0) + 1)
    val validationError = currentTask.validate()
    if (validationError == null) {
      // TODO(#1146): Handle the scenario when the user clicks next on the last Task. This will
      //  include persisting the list of responses to the database
      currentPosition.postValue((currentPosition.value ?: 0) + 1)
      //      currentPosition.value = currentPosition.value!! + 1

      responses[currentTask.task.id] = currentTask.response.value?.orElse(null)
      return Single.never()
    } else {
      popups.get().showError(validationError)
    }

    return Single.just(validationError)
  }
}
