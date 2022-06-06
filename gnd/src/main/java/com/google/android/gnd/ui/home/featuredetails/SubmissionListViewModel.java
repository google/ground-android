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

package com.google.android.gnd.ui.home.featuredetails;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.submission.Submission;
import com.google.android.gnd.model.task.Task;
import com.google.android.gnd.repository.SubmissionRepository;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.Single;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

public class SubmissionListViewModel extends AbstractViewModel {

  @Hot(replays = true)
  public final MutableLiveData<Boolean> isLoading = new MutableLiveData(false);

  private final SubmissionRepository submissionRepository;

  @Hot
  private FlowableProcessor<SubmissionListRequest> submissionListRequests =
      PublishProcessor.create();

  private LiveData<ImmutableList<Submission>> submissionList;

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

  public LiveData<ImmutableList<Submission>> getSubmissions() {
    return submissionList;
  }

  /**
   * Loads a list of submissions associated with a given feature.
   */
  public void loadSubmissionList(Feature feature) {
    Optional<Task> form = feature.getJob().getTask();
    loadSubmissions(feature.getSurvey(), feature.getId(), form.map(Task::getId));
  }

  private Single<ImmutableList<Submission>> getSubmissions(SubmissionListRequest req) {
    if (req.taskId.isEmpty()) {
      // Do nothing. No task defined for this layer.
      // TODO(#354): Show message or special treatment for layer with no task.
      return Single.just(ImmutableList.of());
    }
    return submissionRepository
        .getSubmissions(req.survey.getId(), req.featureId, req.taskId.get())
        .onErrorResumeNext(this::onGetSubmissionsError);
  }

  private Single<ImmutableList<Submission>> onGetSubmissionsError(Throwable t) {
    // TODO: Show an appropriate error message to the user.
    Timber.e(t, "Failed to fetch submission list.");
    return Single.just(ImmutableList.of());
  }

  private void loadSubmissions(Survey survey, String featureId, Optional<String> taskId) {
    submissionListRequests.onNext(new SubmissionListRequest(survey, featureId, taskId));
  }

  static class SubmissionListRequest {

    final Survey survey;
    final String featureId;
    final Optional<String> taskId;

    public SubmissionListRequest(Survey survey, String featureId, Optional<String> taskId) {
      this.survey = survey;
      this.featureId = featureId;
      this.taskId = taskId;
    }
  }
}
