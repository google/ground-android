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

package com.google.android.gnd.ui.editsubmission;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static androidx.lifecycle.LiveDataReactiveStreams.fromPublisher;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.R;
import com.google.android.gnd.model.submission.Response;
import com.google.android.gnd.model.submission.ResponseDelta;
import com.google.android.gnd.model.submission.ResponseMap;
import com.google.android.gnd.model.submission.Submission;
import com.google.android.gnd.model.task.Field;
import com.google.android.gnd.model.task.Step;
import com.google.android.gnd.model.task.Step.Type;
import com.google.android.gnd.model.task.Task;
import com.google.android.gnd.repository.SubmissionRepository;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.PermissionsManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.util.BitmapUtil;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java8.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import timber.log.Timber;

public class EditSubmissionViewModel extends AbstractViewModel {

  // Injected dependencies.

  private final SubmissionRepository submissionRepository;
  private final Resources resources;
  private final PermissionsManager permissionsManager;
  private final BitmapUtil bitmapUtil;

  // States.

  /**
   * True if submission is currently being loaded, otherwise false.
   */
  @Hot(replays = true)
  public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

  /**
   * True if submission is currently being saved, otherwise false.
   */
  @Hot(replays = true)
  public final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);

  /** Task definition, loaded when view is initialized. */
  private final LiveData<Task> task;

  /**
   * Toolbar title, based on whether user is adding new or editing existing submission.
   */
  @Hot(replays = true)
  private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>();

  /**
   * Current task responses.
   */
  private final Map<String, Response> responses = new HashMap<>();

  /**
   * Task validation errors, updated when existing for loaded and when responses change.
   */
  @Nullable
  private Map<String, String> validationErrors;

  /**
   * Arguments passed in from view on initialize().
   */
  @Hot(replays = true)
  private final FlowableProcessor<EditSubmissionFragmentArgs> viewArgs = BehaviorProcessor.create();

  /**
   * Submission state loaded when view is initialized.
   */
  @Nullable
  private Submission originalSubmission;

  /**
   * True if the submission is being added, false if editing an existing one.
   */
  private boolean isNew;

  /**
   * Emits the last photo field id updated and either its photo result, or empty if removed. The
   * last value is emitted on each subscription because {@see #onPhotoResult} is called before
   * subscribers are created.
   */
  private final Subject<PhotoResult> lastPhotoResult = BehaviorSubject.create();

  // Events.

  /**
   * "Save" button clicks.
   */
  @Hot
  private final PublishProcessor<Nil> saveClicks = PublishProcessor.create();

  /**
   * Outcome of user clicking "Save".
   */
  private final Observable<SaveResult> saveResults;

  /**
   * Field id waiting for a photo response. As only 1 photo result is returned at a time, we can
   * directly map it 1:1 with the field waiting for a photo response.
   */
  @Nullable
  private String fieldWaitingForPhoto;

  /**
   * Full path of the captured photo in local storage. In case of selecting a photo from storage,
   * URI is returned. But when capturing a photo using camera, we need to pass a valid URI and the
   * result returns true/false based on whether the operation passed or not. As only 1 photo result
   * is returned at a time, we can directly map it 1:1 with the path of the captured photo.
   */
  @Nullable
  private String capturedPhotoPath;

  @Inject
  EditSubmissionViewModel(
      Resources resources,
      SubmissionRepository submissionRepository,
      PermissionsManager permissionsManager,
      BitmapUtil bitmapUtil) {
    this.resources = resources;
    this.submissionRepository = submissionRepository;
    this.permissionsManager = permissionsManager;
    this.bitmapUtil = bitmapUtil;
    this.task = fromPublisher(viewArgs.switchMapSingle(this::onInitialize));
    this.saveResults = saveClicks.toObservable().switchMapSingle(__ -> onSave());
  }

  private static boolean isAddSubmissionRequest(EditSubmissionFragmentArgs args) {
    return args.getSubmissionId().isEmpty();
  }

  public LiveData<Task> getTask() {
    return task;
  }

  public LiveData<String> getToolbarTitle() {
    return toolbarTitle;
  }

  Observable<SaveResult> getSaveResults() {
    return saveResults;
  }

  public @Nullable
      String getSurveyId() {
    return originalSubmission == null ? null : originalSubmission.getSurvey().getId();
  }

  public @Nullable
      String getSubmissionId() {
    return originalSubmission == null ? null : originalSubmission.getId();
  }

  void initialize(EditSubmissionFragmentArgs args) {
    viewArgs.onNext(args);
  }

  Optional<Response> getResponse(String fieldId) {
    return Optional.ofNullable(responses.get(fieldId));
  }

  /**
   * Update the current value of a response. Called what fields are initialized and on each
   * subsequent change.
   */
  void setResponse(Field field, Optional<Response> newResponse) {
    newResponse.ifPresentOrElse(
        r -> responses.put(field.getId(), r), () -> responses.remove(field.getId()));
  }

  @Cold
  public Completable obtainCapturePhotoPermissions() {
    return permissionsManager
        .obtainPermission(WRITE_EXTERNAL_STORAGE)
        .andThen(permissionsManager.obtainPermission(CAMERA));
  }

  @Cold
  public Completable obtainSelectPhotoPermissions() {
    return permissionsManager.obtainPermission(READ_EXTERNAL_STORAGE);
  }

  public void onSaveClick(Map<String, String> validationErrors) {
    this.validationErrors = validationErrors;
    saveClicks.onNext(Nil.NIL);
  }

  private Single<Task> onInitialize(EditSubmissionFragmentArgs viewArgs) {
    isLoading.setValue(true);
    isNew = isAddSubmissionRequest(viewArgs);
    Single<Submission> obs;
    if (isNew) {
      toolbarTitle.setValue(resources.getString(R.string.add_submission_toolbar_title));
      obs = createSubmission(viewArgs);
    } else {
      toolbarTitle.setValue(resources.getString(R.string.edit_submission));
      obs = loadSubmission(viewArgs);
    }
    HashMap<String, Response> restoredResponses = viewArgs.getRestoredResponses();
    return obs.doOnSuccess(
            loadedSubmission -> onSubmissionLoaded(loadedSubmission, restoredResponses))
        .map(Submission::getTask);
  }

  private void onSubmissionLoaded(
      Submission submission, @Nullable Map<String, Response> restoredResponses) {
    Timber.v("Submission loaded");
    this.originalSubmission = submission;
    responses.clear();
    if (restoredResponses == null) {
      ResponseMap responseMap = submission.getResponses();
      for (String fieldId : responseMap.fieldIds()) {
        responseMap.getResponse(fieldId).ifPresent(r -> responses.put(fieldId, r));
      }
    } else {
      Timber.v("Restoring responses from bundle");
      responses.putAll(restoredResponses);
    }
    isLoading.postValue(false);
  }

  private Single<Submission> createSubmission(EditSubmissionFragmentArgs args) {
    return submissionRepository
        .createSubmission(args.getSurveyId(), args.getFeatureId(), args.getTaskId())
        .onErrorResumeNext(this::onError);
  }

  private Single<Submission> loadSubmission(EditSubmissionFragmentArgs args) {
    return submissionRepository
        .getSubmission(args.getSurveyId(), args.getFeatureId(), args.getSubmissionId())
        .onErrorResumeNext(this::onError);
  }

  private Single<SaveResult> onSave() {
    if (originalSubmission == null) {
      Timber.e("Save attempted before submission loaded");
      return Single.just(SaveResult.NO_CHANGES_TO_SAVE);
    }

    if (hasValidationErrors()) {
      return Single.just(SaveResult.HAS_VALIDATION_ERRORS);
    }
    if (!hasUnsavedChanges()) {
      return Single.just(SaveResult.NO_CHANGES_TO_SAVE);
    }
    return save();
  }

  private <T> Single<T> onError(Throwable throwable) {
    // TODO: Refactor and stream to UI.
    Timber.e(throwable, "Error");
    return Single.never();
  }

  private Single<SaveResult> save() {
    if (originalSubmission == null) {
      return Single.error(new IllegalStateException("Submission is null"));
    }

    return submissionRepository
        .createOrUpdateSubmission(originalSubmission, getResponseDeltas(), isNew)
        .doOnSubscribe(__ -> isSaving.postValue(true))
        .doOnComplete(() -> isSaving.postValue(false))
        .toSingleDefault(SaveResult.SAVED);
  }

  private ImmutableList<ResponseDelta> getResponseDeltas() {
    if (originalSubmission == null) {
      Timber.e("Response diff attempted before submission loaded");
      return ImmutableList.of();
    }
    ImmutableList.Builder<ResponseDelta> deltas = ImmutableList.builder();
    ResponseMap originalResponses = originalSubmission.getResponses();
    Timber.v("Responses:\n Before: %s \nAfter:  %s", originalResponses, responses);
    for (Step e : originalSubmission.getTask().getSteps()) {
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

  public Serializable getDraftResponses() {
    return new HashMap<>(responses);
  }

  @Nullable
  public String getFieldWaitingForPhoto() {
    return fieldWaitingForPhoto;
  }

  public void setFieldWaitingForPhoto(@Nullable String fieldWaitingForPhoto) {
    this.fieldWaitingForPhoto = fieldWaitingForPhoto;
  }

  @Nullable
  public String getCapturedPhotoPath() {
    return capturedPhotoPath;
  }

  public void setCapturedPhotoPath(@Nullable String photoUri) {
    this.capturedPhotoPath = photoUri;
  }

  public Observable<PhotoResult> getLastPhotoResult() {
    return lastPhotoResult;
  }

  public void onSelectPhotoResult(Uri uri) {
    if (uri == null) {
      Timber.v("Select photo failed or canceled");
      return;
    }
    if (fieldWaitingForPhoto == null) {
      Timber.e("Photo captured but no field waiting for the result");
      return;
    }
    try {
      onPhotoResult(PhotoResult.createSelectResult(fieldWaitingForPhoto, bitmapUtil.fromUri(uri)));
      Timber.v("Select photo result returned");
    } catch (IOException e) {
      Timber.e(e, "Error getting photo selected from storage");
    }
  }

  public void onCapturePhotoResult(boolean result) {
    if (!result) {
      Timber.v("Capture photo failed or canceled");
      // TODO: Cleanup created file if it exists.
      return;
    }
    if (fieldWaitingForPhoto == null) {
      Timber.e("Photo captured but no field waiting for the result");
      return;
    }
    if (capturedPhotoPath == null) {
      Timber.e("Photo captured but no path available to read the result");
      return;
    }
    onPhotoResult(PhotoResult.createCaptureResult(fieldWaitingForPhoto, capturedPhotoPath));
    Timber.v("Photo capture result returned");
  }

  private void onPhotoResult(PhotoResult result) {
    capturedPhotoPath = null;
    fieldWaitingForPhoto = null;
    lastPhotoResult.onNext(result);
  }

  public void clearPhoto(String fieldId) {
    lastPhotoResult.onNext(PhotoResult.createEmptyResult(fieldId));
  }

  /**
   * Possible outcomes of user clicking "Save".
   */
  enum SaveResult {
    HAS_VALIDATION_ERRORS,
    NO_CHANGES_TO_SAVE,
    SAVED
  }

  @AutoValue
  abstract static class PhotoResult {

    boolean isHandled;

    abstract String getFieldId();

    abstract Optional<Bitmap> getBitmap();

    abstract Optional<String> getPath();

    public boolean isHandled() {
      return isHandled;
    }

    public boolean hasFieldId(String fieldId) {
      return getFieldId().equals(fieldId);
    }

    public boolean isEmpty() {
      return getBitmap().isEmpty() && getPath().isEmpty();
    }

    public void setHandled(boolean handled) {
      isHandled = handled;
    }

    static PhotoResult createEmptyResult(String fieldId) {
      return new AutoValue_EditSubmissionViewModel_PhotoResult(
          fieldId, Optional.empty(), Optional.empty());
    }

    static PhotoResult createSelectResult(String fieldId, Bitmap bitmap) {
      return new AutoValue_EditSubmissionViewModel_PhotoResult(
          fieldId, Optional.of(bitmap), Optional.empty());
    }

    static PhotoResult createCaptureResult(String fieldId, String path) {
      return new AutoValue_EditSubmissionViewModel_PhotoResult(
          fieldId, Optional.empty(), Optional.of(path));
    }
  }
}
