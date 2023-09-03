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
package com.google.android.ground.ui.home.locationofinterestdetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.toLiveData
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import io.reactivex.Single
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import java8.util.Optional
import javax.inject.Inject
import timber.log.Timber

class SubmissionListViewModel
@Inject
constructor(private val submissionRepository: SubmissionRepository) : AbstractViewModel() {
  @JvmField val isLoading: @Hot(replays = true) MutableLiveData<Boolean> = MutableLiveData(false)
  private val submissionListRequests: @Hot FlowableProcessor<SubmissionListRequest> =
    PublishProcessor.create()
  val submissions: LiveData<List<Submission?>?>

  init {
    submissions =
      submissionListRequests
        .doOnNext { isLoading.postValue(true) }
        .switchMapSingle { req: SubmissionListRequest -> getSubmissions(req) }
        .doOnNext { isLoading.postValue(false) }
        .toLiveData()
  }

  /** Loads a list of submissions associated with a given locationOfInterest. */
  fun loadSubmissionList(locationOfInterest: LocationOfInterest) {
    loadSubmissions(
      locationOfInterest.surveyId,
      locationOfInterest.id,
      Optional.of(locationOfInterest.job).map(Job::id)
    )
  }

  private fun getSubmissions(req: SubmissionListRequest): Single<List<Submission?>?> {
    if (req.taskId.isEmpty) {
      // Do nothing. No task defined for this layer.
      // TODO(#354): Show message or special treatment for layer with no task.
      return Single.just(listOf<Submission>())
    }
    throw UnsupportedOperationException("Convert to Kotlin and use appropriate method")
  }

  private fun onGetSubmissionsError(t: Throwable): Single<List<Submission>> {
    // TODO: Show an appropriate error message to the user.
    Timber.e(t, "Failed to fetch submission list.")
    return Single.just(listOf())
  }

  private fun loadSubmissions(
    surveyId: String,
    locationOfInterestId: String,
    taskId: Optional<String>
  ) {
    submissionListRequests.onNext(SubmissionListRequest(surveyId, locationOfInterestId, taskId))
  }

  internal class SubmissionListRequest(
    val surveyId: String,
    val locationOfInterestId: String,
    val taskId: Optional<String>
  )
}
