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

package com.google.android.gnd.ui.editobservation;

import static androidx.lifecycle.LiveDataReactiveStreams.fromPublisher;

import android.content.res.Resources;
import android.view.View;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.repository.ObservationRepository;
import com.google.android.gnd.rx.Event;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.PublishProcessor;
import java.util.Date;
import java.util.Map;
import java8.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import timber.log.Timber;

// TODO: Save draft to local db on each change.
@SharedViewModel
public class EditObservationViewModel extends AbstractViewModel {

  // TODO: Move out of id and into fragment args.
  private static final String ADD_OBSERVATION_ID_PLACEHOLDER = "NEW";

  // Injected inputs.

  private final ObservationRepository observationRepository;
  private final AuthenticationManager authManager;
  private final Resources resources;

  // Input events.

  /** Arguments passed in from view on initialize(). */
  private final BehaviorProcessor<EditObservationFragmentArgs> viewArgs =
      BehaviorProcessor.create();

  /** "Save" button clicks. */
  private final PublishProcessor<Map<Field, Optional<Response>>> saveClicks =
      PublishProcessor.create();

  // View state streams.

  /** Form definition, loaded when view is initialized. */
  private final LiveData<Form> form;

  /** Toolbar title, based on whether user is adding new or editing existing observation. */
  private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>();

  /** Visibility of process widget shown while loading. */
  private final MutableLiveData<Integer> loadingSpinnerVisibility =
      new MutableLiveData<>(View.GONE);

  /** Visibility of "Save" button hidden while loading. */
  private final MutableLiveData<Integer> saveButtonVisibility = new MutableLiveData<>(View.GONE);

  /** Visibility of saving progress dialog, show saving. */
  private final MutableLiveData<Integer> savingProgressVisibility =
      new MutableLiveData<>(View.GONE);

  /** Outcome of user clicking "Save". */
  private final LiveData<Event<SaveResult>> saveResults;

  // Internal state.

  /** Observation state loaded when view is initialized. */
  @Nullable private Observation originalObservation;

  /** True if the observation is being added, false if editing an existing one. */
  private boolean isNew;

  @Inject
  EditObservationViewModel(
      GndApplication application,
      ObservationRepository observationRepository,
      AuthenticationManager authenticationManager) {
    this.resources = application.getResources();
    this.observationRepository = observationRepository;
    this.authManager = authenticationManager;
    this.form = fromPublisher(viewArgs.switchMapSingle(this::onInitialize));
    this.saveResults = fromPublisher(saveClicks.switchMapSingle(this::onSave));
  }

  private static boolean isAddObservationRequest(EditObservationFragmentArgs args) {
    return args.getObservationId().equals(ADD_OBSERVATION_ID_PLACEHOLDER);
  }

  public LiveData<Form> getForm() {
    return form;
  }

  public LiveData<Integer> getLoadingSpinnerVisibility() {
    return loadingSpinnerVisibility;
  }

  public LiveData<Integer> getSaveButtonVisibility() {
    return saveButtonVisibility;
  }

  public LiveData<Integer> getSavingProgressVisibility() {
    return savingProgressVisibility;
  }

  public LiveData<String> getToolbarTitle() {
    return toolbarTitle;
  }

  LiveData<Event<SaveResult>> getSaveResults() {
    return saveResults;
  }

  void initialize(EditObservationFragmentArgs args) {
    viewArgs.onNext(args);
  }

  ResponseMap getOriginalResponses() {
    if (originalObservation == null) {
      throw new IllegalStateException("Attempted to get responses before observation is loaded");
    }
    return originalObservation.getResponses();
  }

  void onSaveResponses(Map<Field, Optional<Response>> fieldResponseMap) {
    saveClicks.onNext(fieldResponseMap);
  }

  private Single<Form> onInitialize(EditObservationFragmentArgs viewArgs) {
    saveButtonVisibility.setValue(View.GONE);
    loadingSpinnerVisibility.setValue(View.VISIBLE);
    isNew = isAddObservationRequest(viewArgs);
    Single<Observation> obs;
    if (isNew) {
      toolbarTitle.setValue(resources.getString(R.string.add_observation_toolbar_title));
      obs = createObservation(viewArgs);
    } else {
      toolbarTitle.setValue(resources.getString(R.string.edit_observation));
      obs = loadObservation(viewArgs);
    }
    return obs.doOnSuccess(this::onObservationLoaded).map(Observation::getForm);
  }

  private void onObservationLoaded(Observation observation) {
    this.originalObservation = observation;
    saveButtonVisibility.postValue(View.VISIBLE);
    loadingSpinnerVisibility.postValue(View.GONE);
  }

  private Single<Observation> createObservation(EditObservationFragmentArgs args) {
    return observationRepository
        .createObservation(
            args.getProjectId(),
            args.getFeatureId(),
            args.getFormId(),
            authManager.getCurrentUser())
        .onErrorResumeNext(this::onError);
  }

  private Single<Observation> loadObservation(EditObservationFragmentArgs args) {
    return observationRepository
        .getObservation(args.getProjectId(), args.getFeatureId(), args.getObservationId())
        .onErrorResumeNext(this::onError);
  }

  private Single<Event<SaveResult>> onSave(Map<Field, Optional<Response>> fieldResponseMap) {
    ImmutableList<ResponseDelta> responseDeltas = getResponseDeltas(fieldResponseMap);

    // check for empty response delta
    if (responseDeltas.isEmpty()) {
      return Single.just(Event.create(SaveResult.NO_CHANGES_TO_SAVE));
    }

    // check for invalid responses
    for (Field field : fieldResponseMap.keySet()) {
      if (!isValid(field, fieldResponseMap.get(field))) {
        return Single.just(Event.create(SaveResult.HAS_VALIDATION_ERRORS));
      }
    }
    Timber.d("Deltas: %s", responseDeltas);
    return saveResponseDelta(responseDeltas);
  }

  private <T> Single<T> onError(Throwable throwable) {
    // TODO: Refactor and stream to UI.
    Timber.e(throwable, "Error");
    return Single.never();
  }

  private Single<Event<SaveResult>> saveResponseDelta(ImmutableList<ResponseDelta> responseDeltas) {
    savingProgressVisibility.setValue(View.VISIBLE);
    ObservationMutation observationMutation =
        ObservationMutation.builder()
            .setType(isNew ? ObservationMutation.Type.CREATE : ObservationMutation.Type.UPDATE)
            .setProjectId(originalObservation.getProject().getId())
            .setFeatureId(originalObservation.getFeature().getId())
            .setLayerId(originalObservation.getFeature().getLayer().getId())
            .setObservationId(originalObservation.getId())
            .setFormId(originalObservation.getForm().getId())
            .setResponseDeltas(responseDeltas)
            .setClientTimestamp(new Date())
            .setUserId(authManager.getCurrentUser().getId())
            .build();
    return observationRepository
        .applyAndEnqueue(observationMutation)
        .doOnComplete(() -> savingProgressVisibility.postValue(View.GONE))
        .toSingleDefault(Event.create(SaveResult.SAVED));
  }

  private ImmutableList<ResponseDelta> getResponseDeltas(
      Map<Field, Optional<Response>> fieldNewResponsesMap) {
    ImmutableList.Builder<ResponseDelta> deltas = ImmutableList.builder();
    for (Field field : fieldNewResponsesMap.keySet()) {
      Optional<Response> currentResponse = fieldNewResponsesMap.get(field);
      Optional<Response> originalResponse = getOriginalResponses().getResponse(field.getId());
      if (!currentResponse.equals(originalResponse)) {
        deltas.add(
            ResponseDelta.builder()
                .setFieldId(field.getId())
                .setNewResponse(currentResponse)
                .build());
      }
    }
    return deltas.build();
  }

  /** If field is required then response shouldn't be empty. */
  private boolean isValid(Field field, Optional<Response> response) {
    if (!field.isRequired()) {
      return true;
    }
    return response != null && !response.get().isEmpty();
  }

  boolean hasUnsavedChanges(Map<Field, Optional<Response>> fieldResponseMap) {
    return !getResponseDeltas(fieldResponseMap).isEmpty();
  }

  /** Possible outcomes of user clicking "Save". */
  enum SaveResult {
    HAS_VALIDATION_ERRORS,
    NO_CHANGES_TO_SAVE,
    SAVED
  }
}
