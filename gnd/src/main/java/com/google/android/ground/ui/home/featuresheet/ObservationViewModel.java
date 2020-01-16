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

package com.google.android.ground.ui.home.featuresheet;

import android.app.Application;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;
import com.google.android.ground.model.AuditInfo;
import com.google.android.ground.model.User;
import com.google.android.ground.model.observation.Observation;
import com.google.android.ground.ui.common.AbstractViewModel;
import java.util.Date;
import java8.util.function.Consumer;
import javax.inject.Inject;

public class ObservationViewModel extends AbstractViewModel implements OnClickListener {

  public final ObservableField<String> userName;
  public final ObservableField<String> modifiedDate;
  public final ObservableField<String> modifiedTime;
  private final Application application;
  private Consumer<Observation> observationCallback;
  private MutableLiveData<Observation> selectedObservation;

  @Inject
  ObservationViewModel(Application application) {
    this.application = application;
    userName = new ObservableField<>();
    modifiedDate = new ObservableField<>();
    modifiedTime = new ObservableField<>();
    selectedObservation = new MutableLiveData<>();
  }

  @Override
  public void onClick(View view) {
    observationCallback.accept(selectedObservation.getValue());
  }

  public void setObservation(Observation observation) {
    selectedObservation.postValue(observation);

    AuditInfo created = observation.getCreated();
    User createdBy = created.getUser();
    Date creationTime = created.getClientTimeMillis();
    userName.set(createdBy.getDisplayName());
    modifiedDate.set(DateFormat.getMediumDateFormat(application).format(creationTime));
    modifiedTime.set(DateFormat.getTimeFormat(application).format(creationTime));
  }

  void setObservationCallback(Consumer<Observation> observationCallback) {
    this.observationCallback = observationCallback;
  }
}
