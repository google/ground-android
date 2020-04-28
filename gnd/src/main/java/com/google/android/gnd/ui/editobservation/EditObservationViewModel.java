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
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Element.Type;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.repository.ObservationRepository;
import com.google.android.gnd.rx.Event;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.editobservation.field.FieldViewModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.PublishProcessor;
import java.util.Date;
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
  private final PublishProcessor<Nil> saveClicks = PublishProcessor.create();

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

  private FieldViewModel fieldViewModel;

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
    this.saveResults = fromPublisher(saveClicks.switchMapSingle(__ -> onSave()));
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

  void initialize(FieldViewModel fieldViewModel, EditObservationFragmentArgs args) {
    this.fieldViewModel = fieldViewModel;
    fieldViewModel.setProjectId(args.getProjectId());
    fieldViewModel.setFormId(args.getFormId());
    fieldViewModel.setFeatureId(args.getFeatureId());
    fieldViewModel.setObservationId(args.getObservationId());

    viewArgs.onNext(args);
  }

  private Optional<Response> getResponse(String fieldId) {
    return Optional.ofNullable(fieldViewModel.getResponses().get(fieldId));
  }

  public void onSaveClick() {
    saveClicks.onNext(Nil.NIL);
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
    fieldViewModel.setForm(observation.getForm());
    fieldViewModel.setResponses(observation.getForm(), observation.getResponses());
    fieldViewModel.refreshValidationErrors();
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

  private Single<Event<SaveResult>> onSave() {
    if (originalObservation == null) {
      Timber.e("Save attempted before observation loaded");
      return Single.just(Event.create(SaveResult.NO_CHANGES_TO_SAVE));
    }
    fieldViewModel.refreshValidationErrors();
    if (fieldViewModel.hasValidationErrors()) {
      return Single.just(Event.create(SaveResult.HAS_VALIDATION_ERRORS));
    }
    if (!hasUnsavedChanges()) {
      return Single.just(Event.create(SaveResult.NO_CHANGES_TO_SAVE));
    }
    return save();
  }

  private <T> Single<T> onError(Throwable throwable) {
    // TODO: Refactor and stream to UI.
    Timber.e(throwable, "Error");
    return Single.never();
  }

  private Single<Event<SaveResult>> save() {
    savingProgressVisibility.setValue(View.VISIBLE);
    ObservationMutation observationMutation =
        ObservationMutation.builder()
            .setType(isNew ? ObservationMutation.Type.CREATE : ObservationMutation.Type.UPDATE)
            .setProjectId(originalObservation.getProject().getId())
            .setFeatureId(originalObservation.getFeature().getId())
            .setLayerId(originalObservation.getFeature().getLayer().getId())
            .setObservationId(originalObservation.getId())
            .setFormId(originalObservation.getForm().getId())
            .setResponseDeltas(getResponseDeltas())
            .setClientTimestamp(new Date())
            .setUserId(authManager.getCurrentUser().getId())
            .build();
    return observationRepository
        .applyAndEnqueue(observationMutation)
        .doOnComplete(() -> savingProgressVisibility.postValue(View.GONE))
        .toSingleDefault(Event.create(SaveResult.SAVED));
  }

  private ImmutableList<ResponseDelta> getResponseDeltas() {
    if (originalObservation == null) {
      Timber.e("Response diff attempted before observation loaded");
      return ImmutableList.of();
    }
    Builder<ResponseDelta> deltas = ImmutableList.builder();
    ResponseMap originalResponses = originalObservation.getResponses();
    for (Element e : originalObservation.getForm().getElements()) {
      if (e.getType() != Type.FIELD) {
        continue;
      }
      String fieldId = e.getField().getId();
      Optional<Response> originalResponse = originalResponses.getResponse(fieldId);
      Optional<Response> currentResponse = getResponse(fieldId).filter(r -> !r.isEmpty());
      if (currentResponse.equals(originalResponse)) {
        continue;
      }
      deltas.add(
          ResponseDelta.builder().setFieldId(fieldId).setNewResponse(currentResponse).build());
    }
    ImmutableList<ResponseDelta> result = deltas.build();
    Timber.v("Deltas: %s", result);
    return result;
  }

  boolean hasUnsavedChanges() {
    return !getResponseDeltas().isEmpty();
  }

  /** Possible outcomes of user clicking "Save". */
  enum SaveResult {
    HAS_VALIDATION_ERRORS,
    NO_CHANGES_TO_SAVE,
    SAVED
  }
}
