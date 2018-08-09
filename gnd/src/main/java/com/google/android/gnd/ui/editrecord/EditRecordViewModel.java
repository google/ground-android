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
import com.google.android.gnd.vo.Form.Element.Type;
import com.google.android.gnd.vo.Form.Field;
import com.google.android.gnd.vo.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.Record.TextValue;
import com.google.android.gnd.vo.Record.Value;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java8.util.Optional;
import java8.util.stream.Stream;
import javax.inject.Inject;

// TODO: Save draft to local db on each change.
public class EditRecordViewModel extends AbstractViewModel {
  private static final String TAG = EditRecordViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final MutableLiveData<Resource<Record>> record;

  private final Resources resources;
  private final ObservableMap<String, Value> values = new ObservableArrayMap<>();
  private final ObservableMap<String, String> errors = new ObservableArrayMap<>();

  @Inject
  EditRecordViewModel(GndApplication application, DataRepository dataRepository) {
    this.resources = application.getResources();
    this.dataRepository = dataRepository;
    this.record = new MutableLiveData<>();
  }

  LiveData<Resource<Record>> getRecord() {
    return record;
  }

  public Optional<Value> getValue(String fieldId) {
    return Optional.ofNullable(values.get(fieldId));
  }

  public ObservableMap<String, Value> getValues() {
    return values;
  }

  public ObservableMap<String, String> getErrors() {
    return errors;
  }

  public void onTextChanged(Field field, String text) {
    Log.v(TAG, "onTextChanged: " + field.getId());

    onValueChanged(field, TextValue.fromString(text.trim()));
  }

  public void onValueChanged(Field field, Optional<Value> newValue) {
    Log.v(TAG, "onValueChanged: " + field.getId() + " = '" + Value.toString(newValue) + "'");
    newValue.ifPresentOrElse(v -> values.put(field.getId(), v), () -> values.remove(field.getId()));
    updateError(field, newValue);
  }

  void editNewRecord(String projectId, String placeId, String formId) {
    disposeOnClear(
        dataRepository
            .createRecord(projectId, placeId, formId)
            .map(Resource::loaded)
            .doOnSuccess(__ -> reset())
            .subscribe(record::setValue));
  }

  private void reset() {
    values.clear();
    errors.clear();
  }

  private void updateMap(Record r) {
    Log.v(TAG, "Updating map");
    reset();
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
    String fieldId = field.getId();
    Optional<Value> originalValue = r.getValue(fieldId);
    Optional<Value> currentValue = getValue(fieldId);
    if (currentValue.equals(originalValue)) {
      return stream(Collections.emptyList());
    }

    ValueUpdate.Builder update = ValueUpdate.newBuilder();
    update.setElementId(fieldId);
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
            .doOnSuccess(r -> r.getData().ifPresent(this::updateErrors))
            .subscribe(record::setValue));
  }

  public void onFocusChange(Field field, boolean hasFocus) {
    if (!hasFocus) {
      updateError(field);
    }
  }

  private void updateError(Field field) {
    updateError(field, getValue(field.getId()));
  }

  private void updateError(Field field, Optional<Value> value) {
    String key = field.getId();
    if (field.isRequired() && !value.isPresent()) {
      Log.d(TAG, "Missing: " + key);
      errors.put(field.getId(), resources.getString(R.string.required_field));
    } else {
      Log.d(TAG, "Valid: " + key);
      errors.remove(field.getId());
    }
  }

  private void updateErrors(Record r) {
    stream(r.getForm().getElements())
        .filter(e -> e.getType().equals(Type.FIELD))
        .map(e -> e.getField())
        .forEach(this::updateError);
  }
}
