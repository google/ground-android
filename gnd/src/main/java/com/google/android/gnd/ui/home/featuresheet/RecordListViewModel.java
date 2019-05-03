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

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java8.util.Optional;
import java8.util.stream.Collectors;
import javax.inject.Inject;

// TODO: Roll up into parent viewmodel. Simplify VMs overall.
// TODO(#71): Simplify VM project, form, and feature access.
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
            .flatMap(Result.mapObservable(this::fetchRecordSummaries), this::filterByRequestForm)
            .subscribe(Result.unwrap(recordSummaries::setValue, this::onRecordSummaryError)));
  }

  /**
   * Returns the list of current record summaries.
   * @return A list of records.
   */
  public LiveData<List<Record>> getRecordSummaries() {
    return recordSummaries;
  }

  /**
   * Clears the current list of record summaries.
   */
  public void clearRecords() {
    recordSummaries.setValue(Collections.emptyList());
  }

  /**
   * Loads a list of records associated with a given feature and fetches summaries for them.
   * @param feature
   * @param form
   */
  public void loadRecordSummaries(Feature feature, Form form) {
    loadRecords(
        feature.getProject(), feature.getFeatureType().getId(), form.getId(), feature.getId());
  }

  private Observable<List<Record>> fetchRecordSummaries(RecordSummaryRequest request) {
    // TODO: Only fetch records with current formId.
    return dataRepository
        .getRecordSummaries(request.project.getId(), request.featureId)
        .toObservable();
  }

  private Result<List<Record>> filterByRequestForm(
      RecordSummaryRequest request, Result<List<Record>> recordResult) {
    switch (recordResult.getState()) {
      case SUCCESS:
        return Result.success(
            stream(recordResult.get())
                .filter(record -> record.getForm().getId().equals(request.formId))
                .collect(Collectors.toList()));
      case ERROR:
        return Result.error(new Throwable("Failed to filter records by request form ID."));
      default:
        return Result.error(new Throwable("Failed to filter records by request form ID."));
    }
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
