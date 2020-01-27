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
import com.google.android.gnd.repository.Loadable;
import com.google.android.gnd.repository.ObservationRepository;
import com.google.android.gnd.ui.common.AbstractViewModel;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import javax.inject.Inject;

public class ObservationDetailsViewModel extends AbstractViewModel {

  private final BehaviorProcessor<ObservationDetailsFragmentArgs> argsProcessor;
  public final LiveData<Loadable<Observation>> observations;
  public final LiveData<Integer> progressBarVisibility;
  public final LiveData<String> toolbarTitle;
  public final LiveData<String> toolbarSubtitle;

  @Inject
  ObservationDetailsViewModel(ObservationRepository observationRepository) {
    this.argsProcessor = BehaviorProcessor.create();

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

    this.progressBarVisibility =
        LiveDataReactiveStreams.fromPublisher(
            observationStream.map(ObservationDetailsViewModel::getProgressBarVisibility));

    this.toolbarTitle =
        LiveDataReactiveStreams.fromPublisher(
            observationStream.map(ObservationDetailsViewModel::getToolbarTitle));

    this.toolbarSubtitle =
        LiveDataReactiveStreams.fromPublisher(
            observationStream.map(ObservationDetailsViewModel::getToolbarSubtitle));
  }

  public void loadObservationDetails(ObservationDetailsFragmentArgs args) {
    this.argsProcessor.onNext(args);
  }

  private static Integer getProgressBarVisibility(Loadable<Observation> observation) {
    return observation.value().isPresent() ? View.VISIBLE : View.GONE;
  }

  private static String getToolbarTitle(Loadable<Observation> observation) {
    return observation.value().map(Observation::getFeature).map(Feature::getTitle).orElse("");
  }

  private static String getToolbarSubtitle(Loadable<Observation> observation) {
    return observation.value().map(Observation::getFeature).map(Feature::getSubtitle).orElse("");
  }
}
