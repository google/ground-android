package com.google.android.gnd.ui.editobservation.field;

import static com.google.android.gnd.persistence.remote.firestore.FirestoreStorageManager.getRemoteDestinationPath;
import static java8.util.stream.StreamSupport.stream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableMap;
import com.google.android.gnd.Config;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Element.Type;
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
  private String projectId;
  private String formId;
  private String featureId;
  private String observationId;
  private Form form;
  private boolean isPhotoFieldUpdated;

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

  public void setResponses(Form form, ResponseMap responseMap) {
    if (isPhotoFieldUpdated) {
      isPhotoFieldUpdated = false;
    } else {
      responses.clear();
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

  public void refreshValidationErrors() {
    validationErrors.clear();
    stream(form.getElements())
        .filter(e -> e.getType().equals(Type.FIELD))
        .map(Element::getField)
        .forEach(this::updateError);
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

  public boolean hasValidationErrors() {
    return !validationErrors.isEmpty();
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

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public void setFormId(String formId) {
    this.formId = formId;
  }

  public void setFeatureId(String featureId) {
    this.featureId = featureId;
  }

  public void setObservationId(String observationId) {
    this.observationId = observationId;
  }

  public void setForm(Form form) {
    this.form = form;
  }
}
