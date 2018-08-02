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
import android.databinding.ObservableArrayMap;
import android.databinding.ObservableMap;
import android.util.Log;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.Form;
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
  private final ObservableMap<String, String> textValues = new ObservableArrayMap<>();

  @Inject
  EditRecordViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    this.record = new MutableLiveData<>();
    textValues.addOnMapChangedCallback(
      new ObservableMap.OnMapChangedCallback<ObservableMap<String, String>, String, String>() {
        @Override
        public void onMapChanged(ObservableMap<String, String> sender, String key) {

          Log.e("!!!", "Change: " + key);
        }
      });
  }

  LiveData<Resource<Record>> getRecord() {
    return record;
  }

  void editNewRecord(String projectId, String placeId, String formId) {
    disposeOnClear(
      dataRepository
        .createRecord(projectId, placeId, formId)
        .doOnSuccess(this::clearValues)
        .map(Resource::loaded)
        .subscribe(record::setValue));
  }

  private void clearValues(Record record) {
    textValues.clear();
  }

  private void updateMap(Resource<Record> record) {
    record.getData().ifPresent(this::updateMap);
  }

  private void updateMap(Record record) {
    textValues.clear();
    for (String key : record.getValueMap().keySet()) {
      Optional<Record.Value> value = record.getValue(key);
      Optional<Form.Field> field = record.getForm().getField(key);
      field.ifPresent(f -> value.ifPresent(v -> putValue(f, v)));
    }
  }

  private void putValue(Form.Field field, Record.Value value) {
    textValues.put(field.getId(), value.getDetailsText(field));
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
      dataRepository
        .getRecordSnapshot(projectId, placeId, recordId)
        .doOnSuccess(this::updateMap)
        .subscribe(record::setValue));
  }

  public ObservableMap<String, String> getFieldValue() {
    Log.e("!!!", "Get");
    return textValues;
  }

  //  public void setFieldValue(String value) {
  //    Log.e("!!!", "Set " + value);
  //  }
}
