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

package com.google.android.gnd.ui.editobservation;

import android.content.res.Resources;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.common.AbstractViewModel;
import java8.util.Optional;

public class AbstractFieldViewModel extends AbstractViewModel {

  private final Resources resources;
  private final MutableLiveData<String> error = new MutableLiveData<>();
  private final MutableLiveData<String> responseValue = new MutableLiveData<>();

  private Field field;
  private Optional<Response> response;

  AbstractFieldViewModel(GndApplication application) {
    resources = application.getResources();
  }

  public Field getField() {
    return field;
  }

  public void setField(Field field) {
    this.field = field;
  }

  public LiveData<String> getResponseValue() {
    return responseValue;
  }

  public Optional<Response> getResponse() {
    return response;
  }

  public void setResponse(Optional<Response> response) {
    this.response = response;
    responseValue.setValue(response.map(r -> r.getDetailsText(field)).orElse(""));
  }

  public LiveData<String> getError() {
    return error;
  }

  public void setError(String error) {
    this.error.setValue(error);
  }

  public void refreshError() {
    if (field.isRequired() && (response == null || response.isEmpty())) {
      error.setValue(resources.getString(R.string.required_field));
    }
  }
}
