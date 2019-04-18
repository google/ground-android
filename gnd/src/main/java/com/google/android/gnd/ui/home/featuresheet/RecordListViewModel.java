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

import static java8.util.stream.StreamSupport.stream;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import java.util.Collections;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import java8.util.Optional;
import java8.util.stream.Collectors;
import javax.inject.Inject;

// TODO: Roll up into parent viewmodel. Simplify VMs overall.
public class RecordListViewModel extends AbstractViewModel {

  class ArgumentWrapper {
    public Project project;
    public String featureId;
    public String formId;

    public ArgumentWrapper(Project project, String featureId, String formId) {
      this.project = project;
      this.featureId = featureId;
      this.formId = formId;
    }
  }

  private static final String TAG = RecordListViewModel.class.getSimpleName();
  private final DataRepository dataRepository;
  private MutableLiveData<List<Record>> recordSummaries;
  private BehaviorProcessor<ArgumentWrapper> argumentProcessor;

  @Inject
  public RecordListViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    recordSummaries = new MutableLiveData<>();
    argumentProcessor = BehaviorProcessor.create();

    Flowable<Pair<List<Record>, String>> recordsWithFormContext =
        argumentProcessor.flatMap(
            args ->
                dataRepository
                    .getRecordSummaries(args.project.getId(), args.featureId)
                    .toFlowable()
                    .map(records -> Pair.create(records, args.formId)));

    disposeOnClear(
        recordsWithFormContext.subscribe(
            pairs ->
                recordSummaries.setValue(
                    stream(pairs.first)
                        .filter(record -> record.getForm().getId().equals(pairs.second))
                        .collect(Collectors.toList()))));
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

  private void loadRecords(Project project, String featureTypeId, String formId, String featureId) {
    Optional<Form> form = project.getFeatureType(featureTypeId).flatMap(pt -> pt.getForm(formId));
    if (!form.isPresent()) {
      // TODO: Show error.
      return;
    }
    // TODO: Use project id instead of object.
    argumentProcessor.onNext(new ArgumentWrapper(project, featureId, formId));
  }
}
