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

package com.google.android.ground.ui.editsubmission;

import android.content.res.Resources;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.ground.R;
import com.google.android.ground.model.submission.Response;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.rx.annotations.Cold;
import com.google.android.ground.rx.annotations.Hot;
import com.google.android.ground.ui.common.AbstractViewModel;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import java8.util.Optional;

/**
 * Defines the state of an inflated {@link Task} and controls its UI.
 */
public class AbstractFieldViewModel extends AbstractViewModel {

  /**
   * Current value.
   */
  private final LiveData<Optional<Response>> response;

  /**
   * Transcoded text to be displayed for the current {@link AbstractFieldViewModel#response}.
   */
  private final LiveData<String> responseText;

  /**
   * Error message to be displayed for the current {@link AbstractFieldViewModel#response}.
   */
  @Hot(replays = true)
  private final MutableLiveData<String> error = new MutableLiveData<>();

  @Hot(replays = true)
  private final BehaviorProcessor<Optional<Response>> responseSubject = BehaviorProcessor.create();

  private final Resources resources;

  @SuppressWarnings("NullAway.Init")
  private Task task;

  AbstractFieldViewModel(Resources resources) {
    this.resources = resources;
    response = LiveDataReactiveStreams.fromPublisher(responseSubject.distinctUntilChanged());
    responseText = LiveDataReactiveStreams.fromPublisher(getDetailsTextFlowable());
  }

  // TODO: Add a reference of Task in Response for simplification.
  void initialize(Task task, Optional<Response> response) {
    this.task = task;
    setResponse(response);
  }

  @Cold(stateful = true, terminates = false)
  protected final Flowable<String> getDetailsTextFlowable() {
    return responseSubject
        .distinctUntilChanged()
        .map(responseOptional -> responseOptional.map(Response::getDetailsText).orElse(""));
  }

  /**
   * Checks if the current response is valid and updates error value.
   */
  public Optional<String> validate() {
    Optional<String> result = validate(task, responseSubject.getValue());
    error.postValue(result.orElse(null));
    return result;
  }

  // TODO: Check valid response values
  private Optional<String> validate(Task task, Optional<Response> response) {
    if (task.isRequired() && (response == null || response.isEmpty())) {
      return Optional.of(resources.getString(R.string.required_task));
    }
    return Optional.empty();
  }

  public Task getTask() {
    return task;
  }

  public String taskLabel() {
    StringBuilder label = new StringBuilder(task.getLabel());
    if (task.isRequired()) {
      label.append(" *");
    }
    return label.toString();
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

  public void clearResponse() {
    setResponse(Optional.empty());
  }
}
