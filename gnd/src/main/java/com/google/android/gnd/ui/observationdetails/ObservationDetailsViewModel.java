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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.repository.ObservationRepository;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import java8.util.Optional;
import javax.inject.Inject;

public class ObservationDetailsViewModel extends AbstractViewModel {

  public final LiveData<Loadable<Observation>> observations;
  public final LiveData<Boolean> isProgressBarVisible;
  public final LiveData<Optional<Feature>> feature;
  private final ObservationRepository observationRepository;

  @Hot(replays = true)
  private final FlowableProcessor<ObservationDetailsFragmentArgs> argsProcessor =
      BehaviorProcessor.create();

  @Inject
  ObservationDetailsViewModel(ObservationRepository observationRepository) {
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
    this.observations = LiveDataReactiveStreams.fromPublisher(observationStream);

    this.isProgressBarVisible =
        LiveDataReactiveStreams.fromPublisher(
            observationStream.map(ObservationDetailsViewModel::getProgressBarVisibility));

    this.feature =
        LiveDataReactiveStreams.fromPublisher(
            observationStream.map(ObservationDetailsViewModel::getFeature));
  }

  private static Boolean getProgressBarVisibility(Loadable<Observation> observation) {
    return observation.value().isPresent();
  }

  private static Optional<Feature> getFeature(Loadable<Observation> observation) {
    return observation.value().map(Observation::getFeature);
  }

  public void loadObservationDetails(ObservationDetailsFragmentArgs args) {
    this.argsProcessor.onNext(args);
  }

  public Completable deleteCurrentObservation(
      String projectId, String featureId, String observationId) {
    return observationRepository
        .getObservation(projectId, featureId, observationId)
        .flatMapCompletable(observationRepository::deleteObservation);
  }
}
