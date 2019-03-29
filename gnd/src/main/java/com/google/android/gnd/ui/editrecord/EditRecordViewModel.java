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
import static com.google.android.gnd.vo.FeatureUpdate.Operation.CREATE;
import static com.google.android.gnd.vo.FeatureUpdate.Operation.DELETE;
import static com.google.android.gnd.vo.FeatureUpdate.Operation.UPDATE;
import static java8.util.Maps.forEach;
import static java8.util.stream.StreamSupport.stream;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.res.Resources;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableInt;
import androidx.databinding.ObservableMap;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SingleLiveEvent;
import com.google.android.gnd.vo.FeatureUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Form.Element.Type;
import com.google.android.gnd.vo.Form.Field;
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
  private final AuthenticationManager authManager;
  private final MutableLiveData<Resource<Record>> record;
  private final SingleLiveEvent<Void> showUnsavedChangesDialogEvents;
  private final SingleLiveEvent<Void> showErrorDialogEvents;
  private final Resources resources;
  private final ObservableMap<String, Value> values = new ObservableArrayMap<>();
  private final ObservableMap<String, String> errors = new ObservableArrayMap<>();

  public final ObservableInt loadingSpinnerVisibility = new ObservableInt();

  @Inject
  EditRecordViewModel(
      GndApplication application,
      DataRepository dataRepository,
      AuthenticationManager authenticationManager) {
    this.resources = application.getResources();
    this.dataRepository = dataRepository;
    this.record = new MutableLiveData<>();
    this.showUnsavedChangesDialogEvents = new SingleLiveEvent<>();
    this.showErrorDialogEvents = new SingleLiveEvent<>();
    this.authManager = authenticationManager;
  }

  public ObservableMap<String, Value> getValues() {
    return values;
  }

  public Optional<Value> getValue(String fieldId) {
    return Optional.ofNullable(values.get(fieldId));
  }

  public ObservableMap<String, String> getErrors() {
    return errors;
  }

  public void onTextChanged(Field field, String text) {
    Log.v(TAG, "onTextChanged: " + field.getId());

    onValueChanged(field, TextValue.fromString(text));
  }

  public void onValueChanged(Field field, Optional<Value> newValue) {
    Log.v(TAG, "onValueChanged: " + field.getId() + " = '" + Value.toString(newValue) + "'");
    newValue.ifPresentOrElse(v -> values.put(field.getId(), v), () -> values.remove(field.getId()));
    updateError(field, newValue);
  }

  public void onFocusChange(Field field, boolean hasFocus) {
    if (!hasFocus) {
      updateError(field);
    }
  }

  LiveData<Resource<Record>> getRecord() {
    return record;
  }

  LiveData<Void> getShowUnsavedChangesDialogEvents() {
    return showUnsavedChangesDialogEvents;
  }

  public LiveData<Void> getShowErrorDialogEvents() {
    return showErrorDialogEvents;
  }

  void editNewRecord(String projectId, String featureId, String formId) {
    // TODO(#24): Fix leaky subscriptions!
    disposeOnClear(
        dataRepository
            .createRecord(projectId, featureId, formId)
            .map(Resource::loaded)
            .doOnSuccess(__ -> onNewRecordLoaded())
            .subscribe(this::onRecordSnapshot));
  }

  @NonNull
  private Optional<Record> getCurrentRecord() {
    return Resource.getData(record);
  }

  private void onNewRecordLoaded() {
    values.clear();
    errors.clear();
  }

  private void updateMap(Record r) {
    Log.v(TAG, "Updating map");
    values.clear();
    forEach(
        r.getValueMap(),
        (k, v) ->
            r.getForm().getField(k).ifPresent(field -> onValueChanged(field, Optional.of(v))));
  }

  void editExistingRecord(String projectId, String featureId, String recordId) {
    // TODO: Store and retrieve latest edits from cache and/or db.
    // TODO(#24): Fix leaky subscriptions!
    disposeOnClear(
        dataRepository
            .getRecordSnapshot(projectId, featureId, recordId)
            .doOnSuccess(r -> r.data().ifPresent(this::update))
            .subscribe(this::onRecordSnapshot));
  }

  private void onRecordSnapshot(Resource<Record> r) {
    switch (r.operationState().get()) {
      case LOADING:
        loadingSpinnerVisibility.set(View.VISIBLE);
        break;
      case LOADED:
        loadingSpinnerVisibility.set(View.GONE);
        break;
      case SAVING:
        break;
      case SAVED:
        break;
      case NOT_FOUND:
      case ERROR:
        break;
    }
    // TODO: Replace with functional stream.
    record.setValue(r);
  }

  boolean onSaveClick() {
    getCurrentRecord().ifPresent(this::updateErrors);
    if (hasErrors()) {
      showErrorDialogEvents.setValue(null);
      return true;
    }
    if (hasUnsavedChanges()) {
      saveChanges();
      return true;
    }
    return false;
  }

  boolean onBack() {
    if (hasUnsavedChanges()) {
      showUnsavedChangesDialogEvents.setValue(null);
      return true;
    } else {
      return false;
    }
  }

  private void saveChanges() {
    getCurrentRecord().ifPresent(this::saveChanges);
  }

  private void saveChanges(Record r) {
    // TODO(#24): Fix leaky subscriptions!
    disposeOnClear(
        authManager
            .getUser()
            .flatMap(user -> dataRepository.saveChanges(r, getChangeList(r), user))
            .subscribe(record::setValue));
  }

  private Stream<ValueUpdate> getChanges(Record r) {
    return stream(r.getForm().getElements())
        .filter(e -> e.getType() == Type.FIELD)
        .map(e -> e.getField())
        .flatMap(f -> getChanges(r, f));
  }

  private ImmutableList<ValueUpdate> getChangeList(Record r) {
    return getChanges(r).collect(toImmutableList());
  }

  private Stream<ValueUpdate> getChanges(Record r, Field field) {
    String fieldId = field.getId();
    Optional<Value> originalValue = r.getValue(fieldId);
    Optional<Value> currentValue = getValue(fieldId).filter(v -> !v.isEmpty());
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

  private void update(Record record) {
    updateMap(record);
    updateErrors(record);
  }

  private void updateErrors(Record r) {
    errors.clear();
    stream(r.getForm().getElements())
        .filter(e -> e.getType().equals(Type.FIELD))
        .map(e -> e.getField())
        .forEach(this::updateError);
  }

  private void updateError(Field field) {
    updateError(field, getValue(field.getId()));
  }

  private void updateError(Field field, Optional<Value> value) {
    String key = field.getId();
    if (field.isRequired() && !value.filter(v -> !v.isEmpty()).isPresent()) {
      Log.d(TAG, "Missing: " + key);
      errors.put(field.getId(), resources.getString(R.string.required_field));
    } else {
      Log.d(TAG, "Valid: " + key);
      errors.remove(field.getId());
    }
  }

  private boolean hasUnsavedChanges() {
    return getCurrentRecord().map(r -> getChanges(r).findAny().isPresent()).orElse(false);
  }

  private boolean hasErrors() {
    return !errors.isEmpty();
  }
}
