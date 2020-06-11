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
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.common.AbstractViewModel;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import java8.util.Optional;

/** Defines the state of an inflated {@link Field} and controls its UI. */
public class AbstractFieldViewModel extends AbstractViewModel {

  /** Current value. */
  private final LiveData<Optional<Response>> response;

  /** Transcoded text to be displayed for the current {@link AbstractFieldViewModel#response}. */
  private final LiveData<String> responseText;

  /** Error message to be displayed for the current {@link AbstractFieldViewModel#response}. */
  private final LiveData<Optional<String>> error;

  private final BehaviorProcessor<Optional<Response>> responseSubject = BehaviorProcessor.create();
  private final Resources resources;

  private Field field;

  AbstractFieldViewModel(GndApplication application) {
    resources = application.getResources();

    responseText =
        LiveDataReactiveStreams.fromPublisher(
            responseSubject.distinctUntilChanged().switchMapSingle(this::getDetailsText));
    error =
        LiveDataReactiveStreams.fromPublisher(
            responseSubject.distinctUntilChanged().switchMapSingle(this::getErrorText));
    response = LiveDataReactiveStreams.fromPublisher(responseSubject.distinctUntilChanged());
  }

  // TODO: Add a reference of Field in Response for simplification.
  void init(Field field, Optional<Response> response) {
    this.field = field;
    setResponse(response);
  }

  private Single<String> getDetailsText(Optional<Response> responseOptional) {
    return Single.just(responseOptional.map(response -> response.getDetailsText(field)).orElse(""));
  }

  private Single<Optional<String>> getErrorText(Optional<Response> responseOptional) {
    return Single.just(validate(field, responseOptional));
  }

  // TODO: Check valid response values
  private Optional<String> validate(Field field, Optional<Response> response) {
    if (field.isRequired() && (response == null || response.isEmpty())) {
      return Optional.of(resources.getString(R.string.required_field));
    }
    return Optional.empty();
  }

  public Field getField() {
    return field;
  }

  public String fieldLabel() {
    String label = field.getLabel();
    if (field.isRequired()) {
      label += " *";
    }
    return label;
  }

  public LiveData<String> getResponseText() {
    return responseText;
  }

  public LiveData<Optional<String>> getError() {
    return error;
  }

  LiveData<Optional<Response>> getResponse() {
    return response;
  }

  public void setResponse(Optional<Response> response) {
    responseSubject.onNext(response);
  }
}
