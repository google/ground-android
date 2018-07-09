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

import android.arch.lifecycle.MutableLiveData;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Record;
import com.google.common.collect.ImmutableList;
import io.reactivex.Single;
import javax.inject.Inject;

// TODO: Save draft to local db on each change.
public class EditRecordViewModel extends AbstractViewModel {
  private static final String TAG = EditRecordViewModel.class.getSimpleName();
  private final DataRepository dataRepository;
  private final MutableLiveData<Record> record;

  @Inject
  EditRecordViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    this.record = new MutableLiveData<>();
  }

  public Single<Record> createRecord(String projectId, String placeId, String formId) {
    return dataRepository
      .createRecord(projectId, placeId, formId)
      .doOnSuccess(record::setValue);
  }

  public Single<Resource<Record>> getRecordSnapshot(
    String projectId, String placeId, String recordId) {
    return dataRepository
      .getRecordSnapshot(projectId, placeId, recordId)
      .doOnSuccess(r -> r.ifPresent(record::setValue));
  }

  public Single<Record> saveChanges(ImmutableList<ValueUpdate> updates) {
    return dataRepository.saveChanges(record.getValue(), updates);
  }
}
