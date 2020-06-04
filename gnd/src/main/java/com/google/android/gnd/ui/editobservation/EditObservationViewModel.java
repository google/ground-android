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
import static com.google.android.gnd.persistence.remote.firestore.FirestoreStorageManager.getRemoteDestinationPath;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.View;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableMap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.Config;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Element.Type;
import com.google.android.gnd.model.form.Field;
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
import com.google.android.gnd.system.CameraManager;
import com.google.android.gnd.system.StorageManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.PublishProcessor;
import java.io.IOException;
import java.util.Date;
import java8.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import timber.log.Timber;

// TODO: Save draft to local db on each change.
public class EditObservationViewModel extends AbstractViewModel {

  // TODO: Move out of id and into fragment args.
  private static final String ADD_OBSERVATION_ID_PLACEHOLDER = "NEW";

  // Injected inputs.

  private final ObservationRepository observationRepository;
  private final AuthenticationManager authManager;
  private final Resources resources;
  private final StorageManager storageManager;
  private final CameraManager cameraManager;

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

  /** Stream of updates to photo fields. */
  private final MutableLiveData<ImmutableMap<Field, String>> photoUpdates = new MutableLiveData<>();

  /** Original form responses, loaded when view is initialized. */
  private final ObservableMap<String, Response> responses = new ObservableArrayMap<>();

  /** Form validation errors, updated when existing for loaded and when responses change. */
  private final ObservableMap<String, String> validationErrors = new ObservableArrayMap<>();

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

  private EditObservationFragmentArgs args;
  /** Observation state loaded when view is initialized. */
  @Nullable private Observation originalObservation;

  // Internal state.
  /** True if the observation is being added, false if editing an existing one. */
  private boolean isNew;

  @Inject
  EditObservationViewModel(
      GndApplication application,
      ObservationRepository observationRepository,
      AuthenticationManager authenticationManager,
      StorageManager storageManager,
      CameraManager cameraManager) {
    this.resources = application.getResources();
    this.observationRepository = observationRepository;
    this.authManager = authenticationManager;
    this.storageManager = storageManager;
    this.cameraManager = cameraManager;
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

  void initialize(EditObservationFragmentArgs args) {
    viewArgs.onNext(args);
  }

  /** If present, return saved responses. Otherwise, return original responses. */
  Optional<Response> getSavedOrOriginalResponse(String fieldId) {
    if (responses.isEmpty()) {
      return originalObservation.getResponses().getResponse(fieldId);
    } else {
      return Optional.ofNullable(responses.get(fieldId));
    }
  }

  void onErrorChanged(Field field, @Nullable Optional<String> error) {
    if (error != null) {
      error.ifPresentOrElse(
          e -> validationErrors.put(field.getId(), e),
          () -> validationErrors.remove(field.getId()));
    }
  }

  void onResponseChanged(Field field, Optional<Response> newResponse) {
    newResponse.ifPresentOrElse(
        r -> responses.put(field.getId(), r), () -> responses.remove(field.getId()));
  }

  public void showPhotoSelector(Field field) {
    /*
     * Didn't subscribe this with Fragment's lifecycle because we need to retain the disposable
     * after the fragment is destroyed (for activity result)
     */
    // TODO: launch intent through fragment and handle activity result callbacks async
    disposeOnClear(
        storageManager.launchPhotoPicker().andThen(handlePhotoPickerResult(field)).subscribe());
  }

  private Completable handlePhotoPickerResult(Field field) {
    return storageManager
        .photoPickerResult()
        .flatMapCompletable(bitmap -> saveBitmapAndUpdateResponse(bitmap, field));
  }

  public void showPhotoCapture(Field field) {
    /*
     * Didn't subscribe this with Fragment's lifecycle because we need to retain the disposable
     * after the fragment is destroyed (for activity result)
     */
    // TODO: launch intent through fragment and handle activity result callbacks async
    disposeOnClear(
        cameraManager.launchPhotoCapture().andThen(handlePhotoCaptureResult(field)).subscribe());
  }

  private Completable handlePhotoCaptureResult(Field field) {
    return cameraManager
        .capturePhotoResult()
        .flatMapCompletable(bitmap -> saveBitmapAndUpdateResponse(bitmap, field));
  }

  private Completable saveBitmapAndUpdateResponse(Bitmap bitmap, Field field) throws IOException {
    String localFileName = field.getId() + Config.PHOTO_EXT;
    String destinationPath =
        getRemoteDestinationPath(
            args.getProjectId(), args.getFormId(), args.getFeatureId(), localFileName);
    photoUpdates.postValue(ImmutableMap.of(field, destinationPath));
    return storageManager.savePhoto(bitmap, localFileName, destinationPath);
  }

  LiveData<ImmutableMap<Field, String>> getPhotoFieldUpdates() {
    return photoUpdates;
  }

  public void onSaveClick() {
    saveClicks.onNext(Nil.NIL);
  }

  private Single<Form> onInitialize(EditObservationFragmentArgs viewArgs) {
    Single<Observation> obs;

    // Local instance of Observation is cleared once the form is saved.
    // Reuse the same object if the previous form was unsaved.
    // This happens after selecting a photo or resuming app.
    if (originalObservation != null && viewArgs.equals(args)) {
      obs = Single.just(originalObservation);
    } else {
      args = viewArgs;
      isNew = isAddObservationRequest(viewArgs);
      if (isNew) {
        obs = createObservation(viewArgs);
        toolbarTitle.setValue(resources.getString(R.string.add_observation_toolbar_title));
      } else {
        obs = loadObservation(viewArgs);
        toolbarTitle.setValue(resources.getString(R.string.edit_observation));
      }
      reset();
    }

    return obs.doOnSubscribe(__ -> setLoading(true))
        .doOnSuccess(
            observation -> {
              setLoading(false);
              originalObservation = observation;
            })
        .map(Observation::getForm);
  }

  /** Display loading progress bar and hide save button. */
  private void setLoading(boolean isLoading) {
    saveButtonVisibility.postValue(isLoading ? View.GONE : View.VISIBLE);
    loadingSpinnerVisibility.postValue(isLoading ? View.VISIBLE : View.GONE);
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

    if (hasValidationErrors()) {
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
        .doOnSubscribe(__ -> setSaving(true))
        .doOnComplete(
            () -> {
              setSaving(false);
              reset();
            })
        .toSingleDefault(Event.create(SaveResult.SAVED));
  }

  /** Display saving progress bar and hide save button. */
  private void setSaving(boolean isSaving) {
    saveButtonVisibility.postValue(isSaving ? View.GONE : View.VISIBLE);
    savingProgressVisibility.postValue(isSaving ? View.VISIBLE : View.GONE);
  }

  private void reset() {
    responses.clear();
    validationErrors.clear();
    originalObservation = null;
  }

  private ImmutableList<ResponseDelta> getResponseDeltas() {
    if (originalObservation == null) {
      Timber.e("Response diff attempted before observation loaded");
      return ImmutableList.of();
    }
    ImmutableList.Builder<ResponseDelta> deltas = ImmutableList.builder();
    ResponseMap originalResponses = originalObservation.getResponses();
    Timber.v("Responses:\n Before: %s \nAfter:  %s", originalResponses, responses);
    for (Element e : originalObservation.getForm().getElements()) {
      if (e.getType() != Type.FIELD) {
        continue;
      }
      String fieldId = e.getField().getId();
      Optional<Response> originalResponse = originalResponses.getResponse(fieldId);
      Optional<Response> currentResponse =
          getSavedOrOriginalResponse(fieldId).filter(r -> !r.isEmpty());
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

  private boolean hasValidationErrors() {
    return !validationErrors.isEmpty();
  }

  /** Possible outcomes of user clicking "Save". */
  enum SaveResult {
    HAS_VALIDATION_ERRORS,
    NO_CHANGES_TO_SAVE,
    SAVED
  }
}
