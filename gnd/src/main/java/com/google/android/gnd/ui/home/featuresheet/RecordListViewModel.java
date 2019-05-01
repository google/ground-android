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

import static java8.util.stream.StreamSupport.stream;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.rx.Result;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.subjects.PublishSubject;
import java8.util.Optional;
import java8.util.stream.Collectors;
import javax.inject.Inject;

// TODO: Roll up into parent viewmodel. Simplify VMs overall.
public class RecordListViewModel extends AbstractViewModel {

  private static final String TAG = RecordListViewModel.class.getSimpleName();
  private final DataRepository dataRepository;
  private MutableLiveData<List<Record>> recordSummaries;
  private PublishSubject<RecordSummaryRequest> recordSummaryRequests;

  @Inject
  public RecordListViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    recordSummaries = new MutableLiveData<>();
    recordSummaryRequests = PublishSubject.create();

    disposeOnClear(
        recordSummaryRequests
            .switchMapSingle(Result.wrapErrors(this::fetchRecordSummaries))
            .subscribe(Result.unwrapErrors(recordSummaries::setValue, this::onRecordSummaryError)));
  }

  public LiveData<List<Record>> getRecordSummaries() {
    return recordSummaries;
  }

  public void clearRecords() {
    recordSummaries.setValue(Collections.emptyList());
  }

  public void loadRecordSummaries(Feature feature, Form form) {
    loadRecords(
        feature.getProject(), feature.getFeatureType().getId(), form.getId(), feature.getId());
  }

  /**
   * Attempts to fetch a list of records based on the contents of a {@link RecordSummaryRequest}.
   *
   * @param request A record summary request. A triple of project, featureId, and formId.
   * @return A list containing fetched records with forms that satisfy the formId
   *     provided in the request.
   */
  private Single<List<Record>> fetchRecordSummaries(RecordSummaryRequest request) {
    // TODO: Only fetch records with current formId.
    return dataRepository
        .getRecordSummaries(request.project.getId(), request.featureId)
        .map(
            records ->
                stream(records)
                    .filter(record -> record.getForm().getId().equals(request.formId))
                    .collect(Collectors.toList()));
  }

  private void onRecordSummaryError(Throwable t) {
    // TODO: Show an appropriate error message to the user.
    Log.d(TAG, "Failed to fetch record summaries.", t);
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
    public Project project;
    public String featureId;
    public String formId;

    public RecordSummaryRequest(Project project, String featureId, String formId) {
      this.project = project;
      this.featureId = featureId;
      this.formId = formId;
    }
  }
}
