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

package com.google.android.gnd.ui.home.featuresheet;

import android.app.Application;
import android.view.View;
import android.view.View.OnClickListener;
import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.R;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import java.text.DateFormat;
import java.util.Date;
import java8.util.function.Consumer;
import javax.inject.Inject;

public class ObservationViewModel extends AbstractViewModel implements OnClickListener {

  public final ObservableField<String> userName;
  public final ObservableField<String> modifiedDate;
  public final ObservableField<String> modifiedTime;
  private final Application application;
  private Consumer<Observation> recordCallback;
  private MutableLiveData<Observation> selectedRecord;

  @Inject
  ObservationViewModel(Application application) {
    this.application = application;
    userName = new ObservableField<>();
    modifiedDate = new ObservableField<>();
    modifiedTime = new ObservableField<>();
    selectedRecord = new MutableLiveData<>();
  }

  @Override
  public void onClick(View view) {
    recordCallback.accept(selectedRecord.getValue());
  }

  public void setRecord(Observation observation) {
    selectedRecord.postValue(observation);

    AuthenticationManager.User modifiedBy = observation.getModifiedBy();
    // TODO: i18n.
    userName.set(
        modifiedBy == null
            ? application.getApplicationContext().getString(R.string.unknown_user)
            : modifiedBy.getDisplayName());

    if (observation.getServerTimestamps() != null
        && observation.getServerTimestamps().getModified() != null) {
      Date dateModified = observation.getServerTimestamps().getModified();
      DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(application);
      modifiedDate.set(dateFormat.format(dateModified));
      DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(application);
      modifiedTime.set(timeFormat.format(dateModified));
    }
  }

  void setRecordCallback(Consumer<Observation> recordCallback) {
    this.recordCallback = recordCallback;
  }
}
