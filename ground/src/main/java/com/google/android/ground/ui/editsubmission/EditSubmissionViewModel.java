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

package com.google.android.ground.ui.editsubmission;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static androidx.lifecycle.LiveDataReactiveStreams.fromPublisher;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.ground.R;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.submission.Submission;
import com.google.android.ground.model.submission.TaskData;
import com.google.android.ground.model.submission.TaskDataDelta;
import com.google.android.ground.model.submission.TaskDataMap;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.repository.SubmissionRepository;
import com.google.android.ground.rx.Nil;
import com.google.android.ground.rx.annotations.Cold;
import com.google.android.ground.rx.annotations.Hot;
import com.google.android.ground.system.PermissionsManager;
import com.google.android.ground.ui.common.AbstractViewModel;
import com.google.android.ground.ui.util.BitmapUtil;
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

  /** True if submission is currently being loaded, otherwise false. */
  @Hot(replays = true)
  public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
  /** True if submission is currently being saved, otherwise false. */
  @Hot(replays = true)
  public final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);

  private final SubmissionRepository submissionRepository;
  private final Resources resources;

  // States.
  private final PermissionsManager permissionsManager;
  private final BitmapUtil bitmapUtil;
  /** Job definition, loaded when view is initialized. */
  private final LiveData<Job> job;

  /** Toolbar title, based on whether user is adding new or editing existing submission. */
  @Hot(replays = true)
  private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>();

  /** Current task responses. */
  private final Map<String, TaskData> responses = new HashMap<>();
  /** Arguments passed in from view on initialize(). */
  @Hot(replays = true)
  private final FlowableProcessor<EditSubmissionFragmentArgs> viewArgs = BehaviorProcessor.create();
  /**
   * Emits the last photo task id updated and either its photo result, or empty if removed. The last
   * value is emitted on each subscription because {@see #onPhotoResult} is called before
   * subscribers are created.
   */
  private final Subject<PhotoResult> lastPhotoResult = BehaviorSubject.create();
  /** "Save" button clicks. */
  @Hot private final PublishProcessor<Nil> saveClicks = PublishProcessor.create();
  /** Outcome of user clicking "Save". */
  private final Observable<SaveResult> saveResults;
  /** Task validation errors, updated when existing for loaded and when responses change. */
  @Nullable private Map<String, String> validationErrors;

  // Events.
  /** Submission state loaded when view is initialized. */
  @Nullable private Submission originalSubmission;
  /** True if the submission is being added, false if editing an existing one. */
  private boolean isNew;
  /**
   * Task id waiting for a photo taskData. As only 1 photo result is returned at a time, we can
   * directly map it 1:1 with the task waiting for a photo taskData.
   */
  @Nullable private String taskWaitingForPhoto;

  /**
   * Full path of the captured photo in local storage. In case of selecting a photo from storage,
   * URI is returned. But when capturing a photo using camera, we need to pass a valid URI and the
   * result returns true/false based on whether the operation passed or not. As only 1 photo result
   * is returned at a time, we can directly map it 1:1 with the path of the captured photo.
   */
  @Nullable private String capturedPhotoPath;

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
    this.job = fromPublisher(viewArgs.switchMapSingle(this::onInitialize));
    this.saveResults = saveClicks.toObservable().switchMapSingle(__ -> onSave());
  }

  private static boolean isAddSubmissionRequest(EditSubmissionFragmentArgs args) {
    return args.getSubmissionId().isEmpty();
  }

  public LiveData<Job> getJob() {
    return job;
  }

  public LiveData<String> getToolbarTitle() {
    return toolbarTitle;
  }

  Observable<SaveResult> getSaveResults() {
    return saveResults;
  }

  public @Nullable String getSurveyId() {
    return originalSubmission == null ? null : originalSubmission.getSurveyId();
  }

  public @Nullable String getSubmissionId() {
    return originalSubmission == null ? null : originalSubmission.getId();
  }

  void initialize(EditSubmissionFragmentArgs args) {
    viewArgs.onNext(args);
  }

  Optional<TaskData> getResponse(String taskId) {
    return Optional.ofNullable(responses.get(taskId));
  }

  /**
   * Update the current value of a taskData. Called what tasks are initialized and on each
   * subsequent change.
   */
  void setResponse(Task task, Optional<TaskData> newResponse) {
    newResponse.ifPresentOrElse(
        r -> responses.put(task.getId(), r), () -> responses.remove(task.getId()));
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

  private Single<Job> onInitialize(EditSubmissionFragmentArgs viewArgs) {
    isLoading.setValue(true);
    isNew = isAddSubmissionRequest(viewArgs);
    Single<Submission> submissionSingle;
    if (isNew) {
      toolbarTitle.setValue(resources.getString(R.string.add_submission_toolbar_title));
      submissionSingle = createSubmission(viewArgs);
    } else {
      toolbarTitle.setValue(resources.getString(R.string.edit_submission));
      submissionSingle = loadSubmission(viewArgs);
    }
    HashMap<String, TaskData> restoredResponses = viewArgs.getRestoredResponses();
    return submissionSingle
        .doOnSuccess(loadedSubmission -> onSubmissionLoaded(loadedSubmission, restoredResponses))
        .map(Submission::getJob);
  }

  private void onSubmissionLoaded(
      Submission submission, @Nullable Map<String, TaskData> restoredResponses) {
    Timber.v("Submission loaded");
    this.originalSubmission = submission;
    responses.clear();
    if (restoredResponses == null) {
      TaskDataMap taskDataMap = submission.getResponses();
      for (String taskId : taskDataMap.taskIds()) {
        taskDataMap.getResponse(taskId).ifPresent(r -> responses.put(taskId, r));
      }
    } else {
      Timber.v("Restoring responses from bundle");
      responses.putAll(restoredResponses);
    }
    isLoading.postValue(false);
  }

  private Single<Submission> createSubmission(EditSubmissionFragmentArgs args) {
    return submissionRepository
        .createSubmission(args.getSurveyId(), args.getLocationOfInterestId(), args.getJobId())
        .onErrorResumeNext(this::onError);
  }

  private Single<Submission> loadSubmission(EditSubmissionFragmentArgs args) {
    return submissionRepository
        .getSubmission(args.getSurveyId(), args.getLocationOfInterestId(), args.getSubmissionId())
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

  private ImmutableList<TaskDataDelta> getResponseDeltas() {
    if (originalSubmission == null) {
      Timber.e("TaskData diff attempted before submission loaded");
      return ImmutableList.of();
    }
    ImmutableList.Builder<TaskDataDelta> deltas = ImmutableList.builder();
    TaskDataMap originalResponses = originalSubmission.getResponses();
    Timber.v("Responses:\n Before: %s \nAfter:  %s", originalResponses, responses);
    for (Task task : originalSubmission.getJob().getTasksSorted()) {
      String taskId = task.getId();
      Optional<TaskData> originalResponse = originalResponses.getResponse(taskId);
      Optional<TaskData> currentResponse = getResponse(taskId).filter(r -> !r.isEmpty());
      if (currentResponse.equals(originalResponse)) {
        continue;
      }
      deltas.add(new TaskDataDelta(taskId, task.getType(), currentResponse));
    }
    ImmutableList<TaskDataDelta> result = deltas.build();
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
  public String getTaskWaitingForPhoto() {
    return taskWaitingForPhoto;
  }

  public void setTaskWaitingForPhoto(@Nullable String taskWaitingForPhoto) {
    this.taskWaitingForPhoto = taskWaitingForPhoto;
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
    if (taskWaitingForPhoto == null) {
      Timber.e("Photo captured but no task waiting for the result");
      return;
    }
    try {
      onPhotoResult(PhotoResult.createSelectResult(taskWaitingForPhoto, bitmapUtil.fromUri(uri)));
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
    if (taskWaitingForPhoto == null) {
      Timber.e("Photo captured but no task waiting for the result");
      return;
    }
    if (capturedPhotoPath == null) {
      Timber.e("Photo captured but no path available to read the result");
      return;
    }
    onPhotoResult(PhotoResult.createCaptureResult(taskWaitingForPhoto, capturedPhotoPath));
    Timber.v("Photo capture result returned");
  }

  private void onPhotoResult(PhotoResult result) {
    capturedPhotoPath = null;
    taskWaitingForPhoto = null;
    lastPhotoResult.onNext(result);
  }

  public void clearPhoto(String taskId) {
    lastPhotoResult.onNext(PhotoResult.createEmptyResult(taskId));
  }

  /** Possible outcomes of user clicking "Save". */
  enum SaveResult {
    HAS_VALIDATION_ERRORS,
    NO_CHANGES_TO_SAVE,
    SAVED
  }

  @AutoValue
  public abstract static class PhotoResult {

    boolean isHandled;

    static PhotoResult createEmptyResult(String taskId) {
      return new AutoValue_EditSubmissionViewModel_PhotoResult(
          taskId, Optional.empty(), Optional.empty());
    }

    static PhotoResult createSelectResult(String taskId, Bitmap bitmap) {
      return new AutoValue_EditSubmissionViewModel_PhotoResult(
          taskId, Optional.of(bitmap), Optional.empty());
    }

    static PhotoResult createCaptureResult(String taskId, String path) {
      return new AutoValue_EditSubmissionViewModel_PhotoResult(
          taskId, Optional.empty(), Optional.of(path));
    }

    abstract String getTaskId();

    abstract Optional<Bitmap> getBitmap();

    abstract Optional<String> getPath();

    public boolean isHandled() {
      return isHandled;
    }

    public void setHandled(boolean handled) {
      isHandled = handled;
    }

    public boolean hasTaskId(String taskId) {
      return getTaskId().equals(taskId);
    }

    public boolean isEmpty() {
      return getBitmap().isEmpty() && getPath().isEmpty();
    }
  }
}
