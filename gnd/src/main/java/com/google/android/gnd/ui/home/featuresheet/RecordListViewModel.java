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

import static java8.util.stream.StreamSupport.stream;

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
import java8.util.Optional;
import java8.util.stream.Collectors;
import javax.inject.Inject;

// TODO: Roll up into parent viewmodel. Simplify VMs overall.
public class RecordListViewModel extends AbstractViewModel {
  private static final String TAG = RecordListViewModel.class.getSimpleName();
  private final DataRepository dataRepository;
  private MutableLiveData<List<Record>> recordSummaries;

  @Inject
  public RecordListViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    recordSummaries = new MutableLiveData<>();
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
    // TODO(#24): Fix leaky subscriptions!
    disposeOnClear(
        dataRepository
            .getRecordSummaries(project.getId(), featureId)
            .subscribe(
                // TODO: Only fetch records w/current formId.
                records ->
                    recordSummaries.setValue(
                        stream(records)
                            .filter(record -> record.getForm().getId().equals(formId))
                            .collect(Collectors.toList()))));
  }
}
