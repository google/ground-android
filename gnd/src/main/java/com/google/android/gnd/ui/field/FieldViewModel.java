/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.ui.field;

import static com.google.android.gnd.persistence.remote.firestore.FirestoreStorageManager.getRemoteDestinationPath;

import android.content.res.Resources;
import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableMap;
import com.google.android.gnd.Config;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.android.gnd.system.CameraManager;
import com.google.android.gnd.system.StorageManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SharedViewModel;
import io.reactivex.Completable;
import java.io.IOException;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

@SharedViewModel
public class FieldViewModel extends AbstractViewModel {

  private final Resources resources;
  private final StorageManager storageManager;
  private final CameraManager cameraManager;
  private final ObservableMap<String, Response> responses = new ObservableArrayMap<>();
  private final ObservableMap<String, String> validationErrors = new ObservableArrayMap<>();
  private boolean isPhotoFieldUpdated;

  @Nullable private String projectId;
  @Nullable private String formId;
  @Nullable private String featureId;
  @Nullable private Form form;

  @Inject
  FieldViewModel(
      GndApplication application, StorageManager storageManager, CameraManager cameraManager) {
    this.resources = application.getResources();
    this.storageManager = storageManager;
    this.cameraManager = cameraManager;
  }

  public void onTextChanged(Field field, String text) {
    onResponseChanged(field, TextResponse.fromString(text));
  }

  void onResponseChanged(Field field, Optional<Response> newResponse) {
    newResponse.ifPresentOrElse(
        r -> responses.put(field.getId(), r), () -> responses.remove(field.getId()));
    updateError(field, newResponse);
  }

  public ObservableMap<String, Response> getResponses() {
    return responses;
  }

  public void initialize(
      ResponseMap responseMap, Form form, String projectId, String formId, String featureId) {
    this.form = form;
    this.projectId = projectId;
    this.formId = formId;
    this.featureId = featureId;

    if (isPhotoFieldUpdated) {
      // TODO: Move 'isPhotoFieldUpdated' flag to activity/fragment
      // isPhotoFieldUpdated is set to true before launching photo capture/select intent.
      // This prevents overwriting unsaved fields when the activity is resumed.
      // But this state should be stored in the Activity/Fragment and not in ViewModel.
      isPhotoFieldUpdated = false;
    } else {
      // clear in-memory responses and errors from last loaded form
      responses.clear();
      validationErrors.clear();

      // load saved responses
      for (String fieldId : responseMap.fieldIds()) {
        form.getField(fieldId)
            .ifPresent(field -> onResponseChanged(field, responseMap.getResponse(fieldId)));
      }
    }
  }

  Optional<Response> getResponse(String fieldId) {
    return Optional.ofNullable(responses.get(fieldId));
  }

  public void onFocusChange(Field field, boolean hasFocus) {
    if (!hasFocus) {
      updateError(field);
    }
  }

  private void updateError(Field field) {
    updateError(field, getResponse(field.getId()));
  }

  private void updateError(Field field, Optional<Response> response) {
    String key = field.getId();
    if (field.isRequired() && !response.filter(r -> !r.isEmpty()).isPresent()) {
      Timber.d("Missing: %s", key);
      validationErrors.put(field.getId(), resources.getString(R.string.required_field));
    } else {
      Timber.d("Valid: %s", key);
      validationErrors.remove(field.getId());
    }
  }

  public ObservableMap<String, String> getValidationErrors() {
    return validationErrors;
  }

  public void showPhotoSelector(String fieldId) {
    /*
     * Didn't subscribe this with Fragment's lifecycle because we need to retain the disposable
     * after the fragment is destroyed (for activity result)
     */
    // TODO: launch intent through fragment and handle activity result callbacks async
    disposeOnClear(
        storageManager.launchPhotoPicker().andThen(handlePhotoPickerResult(fieldId)).subscribe());
  }

  private Completable handlePhotoPickerResult(String fieldId) {
    return storageManager
        .photoPickerResult()
        .flatMapCompletable(bitmap -> saveBitmapAndUpdateResponse(bitmap, fieldId));
  }

  public void showPhotoCapture(String fieldId) {
    /*
     * Didn't subscribe this with Fragment's lifecycle because we need to retain the disposable
     * after the fragment is destroyed (for activity result)
     */
    // TODO: launch intent through fragment and handle activity result callbacks async
    disposeOnClear(
        cameraManager.launchPhotoCapture().andThen(handlePhotoCaptureResult(fieldId)).subscribe());
  }

  private Completable handlePhotoCaptureResult(String fieldId) {
    return cameraManager
        .capturePhotoResult()
        .flatMapCompletable(bitmap -> saveBitmapAndUpdateResponse(bitmap, fieldId));
  }

  private Completable saveBitmapAndUpdateResponse(Bitmap bitmap, String fieldId)
      throws IOException {
    String localFileName = fieldId + Config.PHOTO_EXT;
    String destinationPath = getRemoteDestinationPath(projectId, formId, featureId, localFileName);

    // TODO: Handle response after reloading view-model and remove this field
    isPhotoFieldUpdated = true;

    // update observable response map
    onTextChanged(form.getField(fieldId).get(), destinationPath);

    return storageManager.savePhoto(bitmap, localFileName, destinationPath);
  }
}
