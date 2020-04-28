package com.google.android.gnd.ui.editobservation.field;

import android.content.res.Resources;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableMap;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SharedViewModel;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

@SharedViewModel
public class FieldViewModel extends AbstractViewModel {

  private final Resources resources;
  private final ObservableMap<String, Response> responses = new ObservableArrayMap<>();
  private final ObservableMap<String, String> validationErrors = new ObservableArrayMap<>();

  @Inject
  FieldViewModel(GndApplication application) {
    this.resources = application.getResources();
  }

  public void onTextChanged(Field field, String text) {
    onResponseChanged(field, TextResponse.fromString(text));
  }

  public void onResponseChanged(Field field, Optional<Response> newResponse) {
    newResponse.ifPresentOrElse(
        r -> responses.put(field.getId(), r), () -> responses.remove(field.getId()));
    updateError(field, newResponse);
  }

  public ObservableMap<String, Response> getResponses() {
    return responses;
  }

  public void setResponses(ResponseMap responseMap) {
    responses.clear();
    for (String fieldId : responseMap.fieldIds()) {
      responseMap.getResponse(fieldId).ifPresent(response -> responses.put(fieldId, response));
    }
  }

  public Optional<Response> getResponse(String fieldId) {
    return Optional.ofNullable(responses.get(fieldId));
  }

  public void onFocusChange(Field field, boolean hasFocus) {
    if (!hasFocus) {
      updateError(field);
    }
  }

  private void refreshValidationErrors() {
    validationErrors.clear();
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
}
