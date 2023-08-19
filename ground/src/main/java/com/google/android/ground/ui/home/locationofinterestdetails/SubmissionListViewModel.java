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

package com.google.android.ground.ui.home.locationofinterestdetails;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.submission.Submission;
import com.google.android.ground.repository.SubmissionRepository;
import com.google.android.ground.rx.annotations.Hot;
import com.google.android.ground.ui.common.AbstractViewModel;
import io.reactivex.Single;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

public class SubmissionListViewModel extends AbstractViewModel {

  @Hot(replays = true)
  public final MutableLiveData<Boolean> isLoading = new MutableLiveData(false);

  private final SubmissionRepository submissionRepository;

  @Hot
  private final FlowableProcessor<SubmissionListRequest> submissionListRequests =
      PublishProcessor.create();

  private final LiveData<List<Submission>> submissionList;

  @Inject
  public SubmissionListViewModel(SubmissionRepository submissionRepository) {
    this.submissionRepository = submissionRepository;
    submissionList =
        LiveDataReactiveStreams.fromPublisher(
            submissionListRequests
                .doOnNext(__ -> isLoading.postValue(true))
                .switchMapSingle(this::getSubmissions)
                .doOnNext(__ -> isLoading.postValue(false)));
  }

  public LiveData<List<Submission>> getSubmissions() {
    return submissionList;
  }

  /** Loads a list of submissions associated with a given locationOfInterest. */
  public void loadSubmissionList(LocationOfInterest locationOfInterest) {
    loadSubmissions(
        locationOfInterest.getSurveyId(),
        locationOfInterest.getId(),
        Optional.of(locationOfInterest.getJob()).map(Job::getId));
  }

  private Single<List<Submission>> getSubmissions(SubmissionListRequest req) {
    if (req.taskId.isEmpty()) {
      // Do nothing. No task defined for this layer.
      // TODO(#354): Show message or special treatment for layer with no task.
      return Single.just(List.of());
    }

    //    return submissionRepository
    //        .getSubmissions(req.surveyId, req.locationOfInterestId, req.taskId.get())
    //        .onErrorResumeNext(this::onGetSubmissionsError);
    // TODO(#1691): Replace the above call with coroutine once migrated to kotlin
    throw new RuntimeException("Please convert to kotlin and use appropriate method");
  }

  private Single<List<Submission>> onGetSubmissionsError(Throwable t) {
    // TODO: Show an appropriate error message to the user.
    Timber.e(t, "Failed to fetch submission list.");
    return Single.just(List.of());
  }

  private void loadSubmissions(
      String surveyId, String locationOfInterestId, Optional<String> taskId) {
    submissionListRequests.onNext(
        new SubmissionListRequest(surveyId, locationOfInterestId, taskId));
  }

  static class SubmissionListRequest {

    final String surveyId;
    final String locationOfInterestId;
    final Optional<String> taskId;

    public SubmissionListRequest(
        String surveyId, String locationOfInterestId, Optional<String> taskId) {
      this.surveyId = surveyId;
      this.locationOfInterestId = locationOfInterestId;
      this.taskId = taskId;
    }
  }
}
