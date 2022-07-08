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

package com.google.android.ground.ui.submissiondetails;

import android.view.View;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.submission.Submission;
import com.google.android.ground.repository.SubmissionRepository;
import com.google.android.ground.rx.Loadable;
import com.google.android.ground.rx.annotations.Hot;
import com.google.android.ground.ui.common.AbstractViewModel;
import com.google.android.ground.ui.common.LocationOfInterestHelper;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import java8.util.Optional;
import javax.inject.Inject;

public class SubmissionDetailsViewModel extends AbstractViewModel {

  @Hot(replays = true)
  public final LiveData<Loadable<Submission>> submission;

  @Hot(replays = true)
  public final LiveData<Integer> progressBarVisibility;

  @Hot(replays = true)
  public final LiveData<String> title;

  @Hot(replays = true)
  public final LiveData<String> subtitle;

  private final SubmissionRepository submissionRepository;

  @Hot(replays = true)
  private final FlowableProcessor<SubmissionDetailsFragmentArgs> argsProcessor =
      BehaviorProcessor.create();

  @Inject
  SubmissionDetailsViewModel(
      SubmissionRepository submissionRepository,
      LocationOfInterestHelper locationOfInterestHelper) {
    this.submissionRepository = submissionRepository;

    Flowable<Loadable<Submission>> submissionStream =
        argsProcessor.switchMapSingle(
            args ->
                submissionRepository
                    .getSubmission(
                        args.getSurveyId(), args.getLocationOfInterestId(), args.getSubmissionId())
                    .map(Loadable::loaded)
                    .onErrorReturn(Loadable::error));

    // TODO: Refactor to expose the fetched submission directly.
    this.submission = LiveDataReactiveStreams.fromPublisher(submissionStream);

    this.progressBarVisibility =
        LiveDataReactiveStreams.fromPublisher(
            submissionStream.map(SubmissionDetailsViewModel::getProgressBarVisibility));

    this.title =
        LiveDataReactiveStreams.fromPublisher(
            submissionStream
                .map(SubmissionDetailsViewModel::getLocationOfInterest)
                .map(locationOfInterestHelper::getLabel));

    this.subtitle =
        LiveDataReactiveStreams.fromPublisher(
            submissionStream
                .map(SubmissionDetailsViewModel::getLocationOfInterest)
                .map(locationOfInterestHelper::getCreatedBy));
  }

  private static Integer getProgressBarVisibility(Loadable<Submission> submission) {
    return submission.isLoaded() ? View.GONE : View.VISIBLE;
  }

  private static Optional<LocationOfInterest> getLocationOfInterest(
      Loadable<Submission> submission) {
    return submission.value().map(Submission::getLocationOfInterest);
  }

  public void loadSubmissionDetails(SubmissionDetailsFragmentArgs args) {
    this.argsProcessor.onNext(args);
  }

  /**
   * Creates an {@link com.google.android.ground.model.mutation.SubmissionMutation}, marks the
   * locally stored {@link Submission} as DELETED and enqueues a worker to remove the submission
   * from remote {@link com.google.android.ground.persistence.remote.firestore.FirestoreDataStore}.
   */
  @Hot
  public Completable deleteCurrentSubmission(
      String surveyId, String locationOfInterestId, String submissionId) {
    return submissionRepository
        .getSubmission(surveyId, locationOfInterestId, submissionId)
        .flatMapCompletable(submissionRepository::deleteSubmission);
  }
}
