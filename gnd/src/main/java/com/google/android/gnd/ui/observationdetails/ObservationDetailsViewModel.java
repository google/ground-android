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
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Persistable;
import com.google.android.gnd.ui.common.AbstractViewModel;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import javax.inject.Inject;

public class ObservationDetailsViewModel extends AbstractViewModel {

  private final DataRepository dataRepository;
  private final BehaviorProcessor<ObservationDetailsFragmentArgs> argsProcessor;
  public final LiveData<Persistable<Observation>> records;
  public final LiveData<Integer> progressBarVisibility;
  public final LiveData<String> toolbarTitle;
  public final LiveData<String> toolbarSubtitle;
  public final LiveData<String> formNameView;

  @Inject
  ObservationDetailsViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;

    this.argsProcessor = BehaviorProcessor.create();

    Flowable<Persistable<Observation>> recordStream =
        argsProcessor.switchMapSingle(
            args ->
                this.dataRepository
                    .getObservation(args.getProjectId(), args.getFeatureId(), args.getRecordId())
                    .map(Persistable::loaded)
                    .onErrorReturn(Persistable::error));

    // TODO: Refactor to expose the fetched observation directly.
    this.records = LiveDataReactiveStreams.fromPublisher(recordStream);

    this.progressBarVisibility =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(ObservationDetailsViewModel::getProgressBarVisibility));

    this.toolbarTitle =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(ObservationDetailsViewModel::getToolbarTitle));

    this.toolbarSubtitle =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(ObservationDetailsViewModel::getToolbarSubtitle));

    this.formNameView =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(ObservationDetailsViewModel::getFormNameView));
  }

  public void loadRecordDetails(ObservationDetailsFragmentArgs args) {
    this.argsProcessor.onNext(args);
  }

  private static Integer getProgressBarVisibility(Persistable<Observation> record) {
    return record.value().isPresent() ? View.VISIBLE : View.GONE;
  }

  private static String getToolbarTitle(Persistable<Observation> record) {
    return record.value().map(Observation::getFeature).map(Feature::getTitle).orElse("");
  }

  private static String getToolbarSubtitle(Persistable<Observation> record) {
    return record.value().map(Observation::getFeature).map(Feature::getSubtitle).orElse("");
  }

  private static String getFormNameView(Persistable<Observation> record) {
    return record.value().map(Observation::getForm).map(Form::getTitle).orElse("");
  }
}
