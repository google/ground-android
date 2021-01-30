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
import static com.google.common.base.Preconditions.checkNotNull;

import android.app.Application;
import android.content.res.Resources;
import android.graphics.Bitmap;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableMap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.Config;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Element.Type;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.repository.ObservationRepository;
import com.google.android.gnd.rx.Event;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.CameraManager;
import com.google.android.gnd.system.StorageManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import java.util.Map;
import java8.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import timber.log.Timber;

public class EditObservationViewModel extends AbstractViewModel {

  // Injected inputs.
  /** True if observation is currently being loaded, otherwise false. */
  @Hot(replays = true)
  public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
  /** True if observation is currently being saved, otherwise false. */
  @Hot(replays = true)
  public final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);

  private final ObservationRepository observationRepository;
  private final Resources resources;
  private final StorageManager storageManager;

  // Input events.
  private final CameraManager cameraManager;
  private final OfflineUuidGenerator uuidGenerator;

  // View state streams.
  /** Arguments passed in from view on initialize(). */
  @Hot(replays = true)
  private final FlowableProcessor<EditObservationFragmentArgs> viewArgs =
      BehaviorProcessor.create();
  /** "Save" button clicks. */
  @Hot private final PublishProcessor<Nil> saveClicks = PublishProcessor.create();
  /** Form definition, loaded when view is initialized. */
  private final LiveData<Form> form;
  /** Toolbar title, based on whether user is adding new or editing existing observation. */
  @Hot(replays = true)
  private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>();
  /** Stream of updates to photo fields. */
  @Hot(replays = true)
  private final MutableLiveData<ImmutableMap<Field, String>> photoUpdates = new MutableLiveData<>();
  /** Original form responses, loaded when view is initialized. */
  private final ObservableMap<String, Response> responses = new ObservableArrayMap<>();
  /** Outcome of user clicking "Save". */
  private final LiveData<Event<SaveResult>> saveResults;
  /** Form validation errors, updated when existing for loaded and when responses change. */
  @Nullable private Map<String, String> validationErrors;
  /** Observation state loaded when view is initialized. */
  @Nullable private Observation originalObservation;

  // Internal state.
  /** True if the observation is being added, false if editing an existing one. */
  private boolean isNew;

  @Inject
  EditObservationViewModel(
      Application application,
      ObservationRepository observationRepository,
      StorageManager storageManager,
      CameraManager cameraManager,
      OfflineUuidGenerator uuidGenerator) {
    this.resources = application.getResources();
    this.observationRepository = observationRepository;
    this.storageManager = storageManager;
    this.cameraManager = cameraManager;
    this.uuidGenerator = uuidGenerator;
    this.form = fromPublisher(viewArgs.switchMapSingle(this::onInitialize));
    this.saveResults = fromPublisher(saveClicks.switchMapSingle(__ -> onSave()));
  }

  private static boolean isAddObservationRequest(EditObservationFragmentArgs args) {
    return args.getObservationId().isEmpty();
  }

  public LiveData<Form> getForm() {
    return form;
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

  Optional<Response> getSavedOrOriginalResponse(String fieldId) {
    if (responses.isEmpty()) {
      if (originalObservation == null) {
        return Optional.empty();
      } else {
        return originalObservation.getResponses().getResponse(fieldId);
      }
    } else {
      return getResponse(fieldId);
    }
  }

  Optional<Response> getResponse(String fieldId) {
    return Optional.ofNullable(responses.get(fieldId));
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

  private Completable saveBitmapAndUpdateResponse(Bitmap bitmap, Field field) {
    String localFileName = uuidGenerator.generateUuid() + Config.PHOTO_EXT;

    checkNotNull(
        originalObservation, "originalObservation was empty when attempting to save bitmap");

    String remoteDestinationPath =
        getRemoteDestinationPath(
            originalObservation.getProject().getId(),
            originalObservation.getForm().getId(),
            originalObservation.getFeature().getId(),
            localFileName);

    photoUpdates.postValue(ImmutableMap.of(field, remoteDestinationPath));

    return storageManager.savePhoto(bitmap, localFileName);
  }

  LiveData<ImmutableMap<Field, String>> getPhotoFieldUpdates() {
    return photoUpdates;
  }

  public void onSave(Map<String, String> validationErrors) {
    this.validationErrors = validationErrors;
    saveClicks.onNext(Nil.NIL);
  }

  private Single<Form> onInitialize(EditObservationFragmentArgs viewArgs) {
    isLoading.setValue(true);
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
    responses.clear();
    isLoading.postValue(false);
  }

  private Single<Observation> createObservation(EditObservationFragmentArgs args) {
    return observationRepository
        .createObservation(args.getProjectId(), args.getFeatureId(), args.getFormId())
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
    if (originalObservation == null) {
      return Single.error(new IllegalStateException("Observation is null"));
    }

    return observationRepository
        .addObservationMutation(originalObservation, getResponseDeltas(), isNew)
        .doOnSubscribe(__ -> isSaving.postValue(true))
        .doOnComplete(() -> isSaving.postValue(false))
        .toSingleDefault(Event.create(SaveResult.SAVED));
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
      Optional<Response> currentResponse = getResponse(fieldId).filter(r -> !r.isEmpty());
      if (currentResponse.equals(originalResponse)) {
        continue;
      }
      deltas.add(
          ResponseDelta.builder()
              .setFieldId(fieldId)
              .setFieldType(e.getField().getType())
              .setNewResponse(currentResponse)
              .build());
    }
    ImmutableList<ResponseDelta> result = deltas.build();
    Timber.v("Deltas: %s", result);
    return result;
  }

  boolean hasUnsavedChanges() {
    return !getResponseDeltas().isEmpty();
  }

  private boolean hasValidationErrors() {
    return validationErrors != null && !validationErrors.isEmpty();
  }

  /** Possible outcomes of user clicking "Save". */
  enum SaveResult {
    HAS_VALIDATION_ERRORS,
    NO_CHANGES_TO_SAVE,
    SAVED
  }
}
