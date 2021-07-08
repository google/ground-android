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

package com.google.android.gnd.ui.observationdetails;

import android.view.View;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.repository.ObservationRepository;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.FeatureHelper;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import java8.util.Optional;
import javax.inject.Inject;

public class ObservationDetailsViewModel extends AbstractViewModel {

  @Hot(replays = true)
  public final LiveData<Loadable<Observation>> observation;

  @Hot(replays = true)
  public final LiveData<Integer> progressBarVisibility;

  @Hot(replays = true)
  public final LiveData<String> title;

  @Hot(replays = true)
  public final LiveData<String> subtitle;

  private final ObservationRepository observationRepository;

  @Hot(replays = true)
  private final FlowableProcessor<ObservationDetailsFragmentArgs> argsProcessor =
      BehaviorProcessor.create();

  @Inject
  ObservationDetailsViewModel(
      ObservationRepository observationRepository, FeatureHelper featureHelper) {
    this.observationRepository = observationRepository;

    Flowable<Loadable<Observation>> observationStream =
        argsProcessor.switchMapSingle(
            args ->
                observationRepository
                    .getObservation(
                        args.getProjectId(), args.getFeatureId(), args.getObservationId())
                    .map(Loadable::loaded)
                    .onErrorReturn(Loadable::error));

    // TODO: Refactor to expose the fetched observation directly.
    this.observation = LiveDataReactiveStreams.fromPublisher(observationStream);

    this.progressBarVisibility =
        LiveDataReactiveStreams.fromPublisher(
            observationStream.map(ObservationDetailsViewModel::getProgressBarVisibility));

    this.title =
        LiveDataReactiveStreams.fromPublisher(
            observationStream
                .map(ObservationDetailsViewModel::getFeature)
                .map(featureHelper::getLabel));

    this.subtitle =
        LiveDataReactiveStreams.fromPublisher(
            observationStream
                .map(ObservationDetailsViewModel::getFeature)
                .map(featureHelper::getCreatedBy));
  }

  private static Integer getProgressBarVisibility(Loadable<Observation> observation) {
    return observation.isLoaded() ? View.GONE : View.VISIBLE;
  }

  private static Optional<Feature> getFeature(Loadable<Observation> observation) {
    return observation.value().map(Observation::getFeature);
  }

  public void loadObservationDetails(ObservationDetailsFragmentArgs args) {
    this.argsProcessor.onNext(args);
  }

  /**
   * Creates an {@link com.google.android.gnd.model.observation.ObservationMutation}, marks the
   * locally stored {@link Observation} as DELETED and enqueues a worker to remove the observation
   * from remote {@link com.google.android.gnd.persistence.remote.firestore.FirestoreDataStore}.
   */
  @Hot
  public Completable deleteCurrentObservation(
      String projectId, String featureId, String observationId) {
    return observationRepository
        .getObservation(projectId, featureId, observationId)
        .flatMapCompletable(observationRepository::deleteObservation);
  }
}
