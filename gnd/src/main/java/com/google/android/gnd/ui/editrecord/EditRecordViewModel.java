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

package com.google.android.gnd.ui.editrecord;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Record;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import javax.inject.Inject;

// TODO: Save draft to local db on each change.
public class EditRecordViewModel extends AbstractViewModel {
  private static final String TAG = EditRecordViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final MutableLiveData<Resource<Record>> record;

  @Inject
  EditRecordViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    this.record = new MutableLiveData<>();
  }

  LiveData<Resource<Record>> getRecord() {
    return record;
  }

  void editNewRecord(String projectId, String placeId, String formId) {
    disposeOnClear(
      dataRepository
        .createRecord(projectId, placeId, formId)
        .map(Resource::loaded)
        .subscribe(record::setValue));
  }

  void saveChanges(ImmutableList<ValueUpdate> updates) {
    Optional<Record> recordData = Resource.getData(record);
    if (!recordData.isPresent()) {
      return;
    }
    disposeOnClear(
      dataRepository.saveChanges(recordData.get(), updates).subscribe(record::setValue));
  }

  void editExistingRecord(String projectId, String placeId, String recordId) {
    // TODO: Store and retrieve latest edits from cache and/or db.
    disposeOnClear(
      dataRepository.getRecordSnapshot(projectId, placeId, recordId).subscribe(record::setValue));
  }
}
