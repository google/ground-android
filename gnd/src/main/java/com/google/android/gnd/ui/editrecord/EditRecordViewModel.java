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
import static java8.util.Maps.forEach;
import static java8.util.stream.StreamSupport.stream;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.res.Resources;
import android.databinding.ObservableArrayMap;
import android.databinding.ObservableMap;
import android.util.Log;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
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
import java8.util.Optional;
import java8.util.stream.Stream;
import javax.inject.Inject;

// TODO: Save draft to local db on each change.
public class EditRecordViewModel extends AbstractViewModel {
  private static final String TAG = EditRecordViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final MutableLiveData<Resource<Record>> record;

  private final Resources resources;
  private final ObservableMap<String, String> textValues = new ObservableArrayMap<>();
  private final ObservableMap<String, String> errors = new ObservableArrayMap<>();
  private final Map<String, Value> values = new HashMap<>();

  @Inject
  EditRecordViewModel(GndApplication application, DataRepository dataRepository) {
    this.resources = application.getResources();
    this.dataRepository = dataRepository;
    this.record = new MutableLiveData<>();
  }

  LiveData<Resource<Record>> getRecord() {
    return record;
  }

  public Optional<Value> getValue(String key) {
    return Optional.ofNullable(values.get(key));
  }

  public ObservableMap<String, String> getTextValues() {
    return textValues;
  }

  public ObservableMap<String, String> getErrors() {
    return errors;
  }

  public void onTextChanged(String key, String text) {
    Log.d(TAG, "onTextChanged: " + key);
    text = text.trim();
    onValueChanged(key, text.isEmpty() ? Optional.empty() : Optional.of(Value.ofText(text)));
  }

  public void onValueChanged(String key, Optional<Value> value) {
    Log.d(TAG, "onValueChanged: " + key);
    Resource.getData(record)
        .map(Record::getForm)
        .flatMap(form -> form.getField(key))
        .ifPresent(
            field -> {
              onValueChanged(field, value);
              validate(field);
            });
  }

  private void onValueChanged(Field field, Optional<Value> newValue) {
    String key = field.getId();
    Optional<Value> prevValue = getValue(field.getId());
    if (prevValue.equals(newValue)) {
      Log.d(TAG, "No change: " + key);
      return;
    }
    String newText = newValue.map(v -> v.getDetailsText(field)).orElse("");
    textValues.put(key, newText);
    newValue.ifPresentOrElse(v -> values.put(key, v), () -> values.remove(key));
    Log.d(TAG, "Value changed: " + key + "  Text: " + newText);
    return;
  }

  void editNewRecord(String projectId, String placeId, String formId) {
    disposeOnClear(
        dataRepository
            .createRecord(projectId, placeId, formId)
            .map(Resource::loaded)
            .doOnSuccess(__ -> clearValues())
            .subscribe(record::setValue));
  }

  private void clearValues() {
    textValues.clear();
    values.clear();
  }

  private void updateMap(Record r) {
    Log.d(TAG, "Updating map");
    clearValues();
    forEach(
        r.getValueMap(),
        (k, v) ->
            r.getForm().getField(k).ifPresent(field -> onValueChanged(field, Optional.of(v))));
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
    Optional<Value> currentValue = getValue(id);
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
            .doOnSuccess(r -> r.getData().ifPresent(this::updateMap))
            .subscribe(record::setValue));
  }

  // TODO: Replace String key with Field?
  public void validate(String key) {
    Resource.getData(record).flatMap(r -> r.getForm().getField(key)).ifPresent(this::validate);
  }

  private void validate(Field field) {
    String key = field.getId();
    Optional<Value> value = Optional.ofNullable(values.get(key));
    if (field.isRequired() && !value.isPresent()) {
      Log.d(TAG, "Missing: " + key);
      errors.put(key, resources.getString(R.string.required_field));
    } else {
      Log.d(TAG, "Valid: " + key);
      errors.remove(key);
    }
  }
}
