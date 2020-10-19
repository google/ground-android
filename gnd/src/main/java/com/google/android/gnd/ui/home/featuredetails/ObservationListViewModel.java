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

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.repository.ObservationRepository;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.Single;
import io.reactivex.processors.PublishProcessor;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

public class ObservationListViewModel extends AbstractViewModel {

  public final ObservableBoolean isLoading = new ObservableBoolean(false);
  private final ObservationRepository observationRepository;
  private PublishProcessor<ObservationListRequest> observationListRequests;
  private LiveData<ImmutableList<Observation>> observationList;

  @Inject
  public ObservationListViewModel(ObservationRepository observationRepository) {
    this.observationRepository = observationRepository;
    observationListRequests = PublishProcessor.create();
    observationList =
        LiveDataReactiveStreams.fromPublisher(
            observationListRequests
                .doOnNext(__ -> isLoading.set(true))
                .switchMapSingle(this::getObservations)
                .doOnNext(__ -> isLoading.set(false)));
  }

  public LiveData<ImmutableList<Observation>> getObservations() {
    return observationList;
  }

  /** Loads a list of observations associated with a given feature. */
  public void loadObservationList(@NonNull Feature feature) {
    Optional<Form> form = feature.getLayer().getForm();
    loadObservations(feature.getProject(), feature.getId(), form.map(Form::getId));
  }

  @NonNull
  private Single<ImmutableList<Observation>> getObservations(@NonNull ObservationListRequest req) {
    if (req.formId.isEmpty()) {
      // Do nothing. No form defined for this layer.
      // TODO: Show text or icon indicating no layers defined.
      return Single.just(ImmutableList.of());
    }
    return observationRepository
        .getObservations(req.project.getId(), req.featureId, req.formId.get())
        .onErrorResumeNext(this::onGetObservationsError);
  }

  @NonNull
  private Single<ImmutableList<Observation>> onGetObservationsError(Throwable t) {
    // TODO: Show an appropriate error message to the user.
    Timber.e(t, "Failed to fetch observation list.");
    return Single.just(ImmutableList.of());
  }

  private void loadObservations(Project project, String featureId, Optional<String> formId) {
    observationListRequests.onNext(new ObservationListRequest(project, featureId, formId));
  }

  static class ObservationListRequest {
    final Project project;
    final String featureId;
    final Optional<String> formId;

    public ObservationListRequest(Project project, String featureId, Optional<String> formId) {
      this.project = project;
      this.featureId = featureId;
      this.formId = formId;
    }
  }
}
