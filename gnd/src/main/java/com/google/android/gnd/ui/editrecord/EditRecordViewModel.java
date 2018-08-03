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

import static com.google.android.gnd.util.Streams.toImmutableList;
import static com.google.android.gnd.vo.PlaceUpdate.Operation.CREATE;
import static com.google.android.gnd.vo.PlaceUpdate.Operation.DELETE;
import static com.google.android.gnd.vo.PlaceUpdate.Operation.UPDATE;
import static java8.util.Maps.getOrDefault;
import static java8.util.stream.StreamSupport.stream;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.databinding.ObservableArrayMap;
import android.databinding.ObservableMap;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.Form.Element;
import com.google.android.gnd.vo.Form.Field;
import com.google.android.gnd.vo.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.Record.Value;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java8.util.Maps;
import java8.util.Optional;
import java8.util.stream.Stream;
import javax.inject.Inject;

// TODO: Save draft to local db on each change.
public class EditRecordViewModel extends AbstractViewModel {
  private static final String TAG = EditRecordViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final MutableLiveData<Resource<Record>> record;

  private final ObservableMap<String, String> textValues = new ObservableArrayMap<>();
  private final Map<String, Value> values = new HashMap<>();

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
        .doOnSuccess(__ -> clearValues())
        .map(Resource::loaded)
        .subscribe(record::setValue));
  }

  private void clearValues() {
    textValues.clear();
    values.clear();
  }

  private void updateMap(Resource<Record> record) {
    record.getData().ifPresent(this::updateMap);
  }

  private void updateMap(Record r) {
    clearValues();
    Maps.forEach(r.getValueMap(), (k, v) -> onValueChanged(r, k, Optional.of(v), false));
  }

  void saveChanges() {
    Resource.getData(record).ifPresent(this::saveChanges);
  }

  private void saveChanges(Record r) {
    ImmutableList<ValueUpdate> updates =
      stream(r.getForm().getElements())
        .filter(e -> e.getType() == Element.Type.FIELD)
        .map(e -> e.getField())
        .flatMap(f -> getChanges(r, f))
        .collect(toImmutableList());
    disposeOnClear(dataRepository.saveChanges(r, updates).subscribe(record::setValue));
  }

  private Stream<ValueUpdate> getChanges(Record r, Field field) {
    String id = field.getId();
    Optional<Value> originalValue = r.getValue(id);
    Optional<Value> currentValue = Optional.ofNullable(values.get(id));
    if (currentValue.equals(originalValue)) {
      return stream(Collections.emptyList());
    }

    ValueUpdate.Builder update = ValueUpdate.newBuilder();
    update.setElementId(id);
    if (!currentValue.isPresent()) {
      update.setOperation(DELETE);
    } else if (originalValue.isPresent()) {
      update.setOperation(UPDATE);
      update.setValue(currentValue);
    } else {
      update.setOperation(CREATE);
      update.setValue(currentValue);
    }
    return stream(Arrays.asList(update.build()));
  }

  void editExistingRecord(String projectId, String placeId, String recordId) {
    // TODO: Store and retrieve latest edits from cache and/or db.
    disposeOnClear(
      dataRepository
        .getRecordSnapshot(projectId, placeId, recordId)
        .doOnSuccess(this::updateMap)
        .subscribe(record::setValue));
  }

  public ObservableMap<String, String> getTextValues() {
    return textValues;
  }

  public void onValueChanged(String key, Optional<Value> value) {
    Resource.getData(record).ifPresent(r -> onValueChanged(r, key, value, true));
  }

  private void onValueChanged(Record r, String key, Optional<Value> value, boolean validate) {
    String prevText = getOrDefault(textValues, key, "");
    String newText =
      r.getForm()
       .getField(key)
       .flatMap(field -> value.map(v -> v.getDetailsText(field)))
       .orElse("");
    if (!prevText.equals(newText)) {
      textValues.put(key, newText);
    }
    value.ifPresentOrElse(v -> values.put(key, v), () -> values.remove(key));
  }

  public void onTextChanged(String key, String text) {
    onValueChanged(key, text.isEmpty() ? Optional.empty() : Optional.of(Value.ofText(text)));
  }
}
