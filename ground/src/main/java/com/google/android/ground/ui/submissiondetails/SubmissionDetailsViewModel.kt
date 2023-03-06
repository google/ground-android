/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.ui.submissiondetails

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.LocationOfInterestHelper
import io.reactivex.Completable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import java8.util.Optional
import javax.inject.Inject

class SubmissionDetailsViewModel
@Inject
internal constructor(
  private val submissionRepository: SubmissionRepository,
  locationOfInterestHelper: LocationOfInterestHelper
) : AbstractViewModel() {

  val progressBarVisibility: @Hot(replays = true) LiveData<Int>
  val submission: @Hot(replays = true) LiveData<Result<Submission>>
  val subtitle: @Hot(replays = true) LiveData<String>
  val title: @Hot(replays = true) LiveData<String>

  private val argsProcessor: @Hot(replays = true) FlowableProcessor<SubmissionDetailsFragmentArgs> =
    BehaviorProcessor.create()

  init {
    val submissionStream =
      argsProcessor.switchMapSingle { args: SubmissionDetailsFragmentArgs ->
        submissionRepository
          .getSubmission(args.surveyId, args.locationOfInterestId, args.submissionId)
          .map { Result.success(it) }
          .onErrorReturn { Result.failure(it) }
      }

    // TODO: Refactor to expose the fetched submission directly.
    submission = LiveDataReactiveStreams.fromPublisher(submissionStream)
    progressBarVisibility =
      LiveDataReactiveStreams.fromPublisher(submissionStream.map { getProgressBarVisibility(it) })
    title =
      LiveDataReactiveStreams.fromPublisher(
        submissionStream
          .map { getLocationOfInterest(it) }
          .map { locationOfInterestHelper.getLabel(it) }
      )
    subtitle =
      LiveDataReactiveStreams.fromPublisher(
        submissionStream
          .map { getLocationOfInterest(it) }
          .map { locationOfInterestHelper.getCreatedBy(it) }
      )
  }

  fun loadSubmissionDetails(args: SubmissionDetailsFragmentArgs) {
    argsProcessor.onNext(args)
  }

  /**
   * Creates an [com.google.android.ground.model.mutation.SubmissionMutation], marks the locally
   * stored [Submission] as DELETED and enqueues a worker to remove the submission from remote
   * [com.google.android.ground.persistence.remote.firebase.FirestoreDataStore].
   */
  fun deleteCurrentSubmission(
    surveyId: String,
    locationOfInterestId: String,
    submissionId: String
  ): @Hot Completable =
    submissionRepository
      .getSubmission(surveyId, locationOfInterestId, submissionId)
      .flatMapCompletable { submissionRepository.deleteSubmission(it) }

  companion object {
    private fun getProgressBarVisibility(submission: Result<Submission>): Int =
      if (submission.isSuccess) View.GONE else View.VISIBLE

    private fun getLocationOfInterest(
      submission: Result<Submission>
    ): Optional<LocationOfInterest> =
      submission.fold(
        onSuccess = { Optional.of(it.locationOfInterest) },
        onFailure = { Optional.empty() }
      )
  }
}
