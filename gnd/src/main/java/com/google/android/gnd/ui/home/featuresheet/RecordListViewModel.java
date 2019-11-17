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
import io.reactivex.schedulers.Schedulers;
import java8.util.Optional;
import javax.inject.Inject;

public class RecordListViewModel extends AbstractViewModel {

  private static final String TAG = RecordListViewModel.class.getSimpleName();
  private final DataRepository dataRepository;
  private PublishProcessor<RecordSummaryRequest> recordSummaryRequests;
  private LiveData<ImmutableList<Record>> recordSummaries;

  public final ObservableInt loadingSpinnerVisibility = new ObservableInt();

  @Inject
  public RecordListViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    recordSummaryRequests = PublishProcessor.create();
    recordSummaries =
        LiveDataReactiveStreams.fromPublisher(
            recordSummaryRequests
                .doOnNext(__ -> loadingSpinnerVisibility.set(View.VISIBLE))
                .switchMapSingle(this::getRecords)
                .doOnNext(__ -> loadingSpinnerVisibility.set(View.GONE)));
  }

  public LiveData<ImmutableList<Record>> getRecordSummaries() {
    return recordSummaries;
  }

  /** Loads a list of records associated with a given feature and fetches summaries for them. */
  public void loadRecordSummaries(Feature feature, Form form) {
    loadRecords(
        feature.getProject(), feature.getFeatureType().getId(), form.getId(), feature.getId());
  }

  private Single<ImmutableList<Record>> getRecords(RecordSummaryRequest req) {
    return dataRepository
        .getRecords(req.project.getId(), req.featureId, req.formId)
        .onErrorResumeNext(this::onGetRecordsError)
        .subscribeOn(Schedulers.io());
  }

  private Single<ImmutableList<Record>> onGetRecordsError(Throwable t) {
    // TODO: Show an appropriate error message to the user.
    Log.d(TAG, "Failed to fetch record summaries.", t);
    return Single.just(ImmutableList.of());
  }

  private void loadRecords(Project project, String featureTypeId, String formId, String featureId) {
    Optional<Form> form = project.getFeatureType(featureTypeId).flatMap(pt -> pt.getForm(formId));
    if (!form.isPresent()) {
      // TODO: Show error.
      return;
    }
    // TODO: Use project id instead of object.
    recordSummaryRequests.onNext(new RecordSummaryRequest(project, featureId, formId));
  }

  class RecordSummaryRequest {
    public final Project project;
    public final String featureId;
    public final String formId;

    public RecordSummaryRequest(Project project, String featureId, String formId) {
      this.project = project;
      this.featureId = featureId;
      this.formId = formId;
    }
  }
}
