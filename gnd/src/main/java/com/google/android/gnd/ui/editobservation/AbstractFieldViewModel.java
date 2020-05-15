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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.ResponseValidator;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import java8.util.Optional;

public class AbstractFieldViewModel extends AbstractViewModel {

  private final ResponseValidator validator;
  private final LiveData<String> error;;
  private final LiveData<Optional<Response>> response;
  private final LiveData<String> responseText;
  private final BehaviorProcessor<Optional<Response>> responseSubject = BehaviorProcessor.create();

  private Field field;

  AbstractFieldViewModel(ResponseValidator validator) {
    this.validator = validator;

    responseText =
        LiveDataReactiveStreams.fromPublisher(
            responseSubject.distinctUntilChanged().switchMapSingle(this::getDetailsText));

    response = LiveDataReactiveStreams.fromPublisher(responseSubject.distinctUntilChanged());

    error =
        LiveDataReactiveStreams.fromPublisher(
            responseSubject.distinctUntilChanged().switchMapMaybe(this::getErrorText));
  }

  void init(Field field, Optional<Response> response) {
    this.field = field;
    setResponse(response);
  }

  private Single<String> getDetailsText(Optional<Response> responseOptional) {
    return Single.just(responseOptional.map(response -> response.getDetailsText(field)).orElse(""));
  }

  private Maybe<String> getErrorText(Optional<Response> responseOptional) {
    String error = validator.validate(field, responseOptional);
    return error != null ? Maybe.just(error) : Maybe.empty();
  }

  public Field getField() {
    return field;
  }

  public LiveData<String> getResponseText() {
    return responseText;
  }

  public LiveData<String> getError() {
    return error;
  }

  LiveData<Optional<Response>> getResponse() {
    return response;
  }

  public void setResponse(Optional<Response> response) {
    responseSubject.onNext(response);
  }

  public void refreshError() {
    responseSubject.onNext(getResponse().getValue());
  }
}
