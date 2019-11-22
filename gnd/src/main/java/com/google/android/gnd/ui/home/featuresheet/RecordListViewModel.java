/*
 * Copyright 2018 Google LLC
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

import android.util.Log;
import android.view.View;
import androidx.databinding.ObservableInt;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Record;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.Single;
import io.reactivex.processors.PublishProcessor;
import java8.util.Optional;
import javax.inject.Inject;

public class RecordListViewModel extends AbstractViewModel {

  private static final String TAG = RecordListViewModel.class.getSimpleName();
  private final DataRepository dataRepository;
  private PublishProcessor<RecordListRequest> recordListRequests;
  private LiveData<ImmutableList<Record>> recordList;

  public final ObservableInt loadingSpinnerVisibility = new ObservableInt();

  @Inject
  public RecordListViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    recordListRequests = PublishProcessor.create();
    recordList =
        LiveDataReactiveStreams.fromPublisher(
            recordListRequests
                .doOnNext(__ -> loadingSpinnerVisibility.set(View.VISIBLE))
                .switchMapSingle(this::getRecords)
                .doOnNext(__ -> loadingSpinnerVisibility.set(View.GONE)));
  }

  public LiveData<ImmutableList<Record>> getRecords() {
    return recordList;
  }

  /** Loads a list of records associated with a given feature. */
  public void loadRecordList(Feature feature, Form form) {
    loadRecords(
        feature.getProject(), feature.getLayer().getId(), form.getId(), feature.getId());
  }

  private Single<ImmutableList<Record>> getRecords(RecordListRequest req) {
    return dataRepository
        .getRecords(req.project.getId(), req.featureId, req.formId)
        .onErrorResumeNext(this::onGetRecordsError);
  }

  private Single<ImmutableList<Record>> onGetRecordsError(Throwable t) {
    // TODO: Show an appropriate error message to the user.
    Log.d(TAG, "Failed to fetch record list.", t);
    return Single.just(ImmutableList.of());
  }

  private void loadRecords(Project project, String layerId, String formId, String featureId) {
    Optional<Form> form = project.getLayer(layerId).flatMap(pt -> pt.getForm(formId));
    if (!form.isPresent()) {
      // TODO: Show error.
      return;
    }
    // TODO: Use project id instead of object.
    recordListRequests.onNext(new RecordListRequest(project, featureId, formId));
  }

  class RecordListRequest {
    public final Project project;
    public final String featureId;
    public final String formId;

    public RecordListRequest(Project project, String featureId, String formId) {
      this.project = project;
      this.featureId = featureId;
      this.formId = formId;
    }
  }
}
