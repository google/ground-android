/*
 * Copyright 2019 Google LLC
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

package com.google.android.gnd.ui.home.featuredetails;

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.Application;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import java.util.Date;
import java8.util.function.Consumer;
import javax.inject.Inject;

public class ObservationViewModel extends AbstractViewModel implements OnClickListener {

  @Hot(replays = true)
  public final MutableLiveData<String> userName = new MutableLiveData<>();

  @Hot(replays = true)
  public final MutableLiveData<String> modifiedDate = new MutableLiveData<>();

  @Hot(replays = true)
  public final MutableLiveData<String> modifiedTime = new MutableLiveData<>();

  private final Application application;

  @Nullable private Consumer<Observation> observationCallback;

  @Hot(replays = true)
  private MutableLiveData<Observation> selectedObservation = new MutableLiveData<>();

  @Inject
  ObservationViewModel(Application application) {
    this.application = application;
  }

  @Override
  public void onClick(View view) {
    checkNotNull(observationCallback, "observationCallback is null");
    observationCallback.accept(selectedObservation.getValue());
  }

  public void setObservation(Observation observation) {
    selectedObservation.postValue(observation);

    AuditInfo created = observation.getCreated();
    User createdBy = created.getUser();
    Date creationTime = created.getClientTimestamp();
    userName.setValue(createdBy.getDisplayName());
    modifiedDate.setValue(DateFormat.getMediumDateFormat(application).format(creationTime));
    modifiedTime.setValue(DateFormat.getTimeFormat(application).format(creationTime));
  }

  void setObservationCallback(Consumer<Observation> observationCallback) {
    this.observationCallback = observationCallback;
  }
}
