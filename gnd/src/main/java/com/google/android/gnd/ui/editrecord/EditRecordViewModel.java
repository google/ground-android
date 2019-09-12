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

import static java8.util.stream.StreamSupport.stream;

import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableInt;
import androidx.databinding.ObservableMap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.R;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Element.Type;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Record;
import com.google.android.gnd.model.observation.RecordMutation;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Persistable;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SingleLiveEvent;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java8.util.Optional;
import javax.inject.Inject;

// TODO: Save draft to local db on each change.
public class EditRecordViewModel extends AbstractViewModel {
  private static final String TAG = EditRecordViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final AuthenticationManager authManager;
  private final MutableLiveData<Persistable<Record>> record;
  private final SingleLiveEvent<Void> showUnsavedChangesDialogEvents;
  private final SingleLiveEvent<Void> showErrorDialogEvents;
  private final Resources resources;
  private final ObservableMap<String, Response> responses = new ObservableArrayMap<>();
  private final ObservableMap<String, String> errors = new ObservableArrayMap<>();
  private final PublishSubject<EditRecordRequest> editRecordRequests;
  private final PublishSubject<SaveRecordRequest> recordSaveRequests;

  public final ObservableInt loadingSpinnerVisibility = new ObservableInt();
  private AuthenticationManager.User currentUser;
  private boolean isNew;

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
    this.editRecordRequests = PublishSubject.create();
    this.recordSaveRequests = PublishSubject.create();

    // TODO(#84): Handle errors on inner stream to avoid breaking outer one.
    // TODO: Simplify this stream and consolidate error handling (remove Resource wrapper?).
    disposeOnClear(
        recordSaveRequests
            .switchMap(
                request ->
                    saveRecord(request)
                        .toObservable()
                        .startWith(Persistable.saving(request.record))
                        .map(__ -> Persistable.saved(request.record))
                        .doOnError(this::onSaveRecordError)
                        // Prevent from breaking upstream.
                        .onErrorResumeNext(Observable.never())
                        .subscribeOn(Schedulers.io()))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(record::setValue));

    disposeOnClear(
        editRecordRequests
            .switchMapSingle(
                record ->
                    createOrUpdateRecord(record)
                        .doOnError(this::onEditRecordError)
                        .onErrorResumeNext(Single.never())
                        .subscribeOn(Schedulers.io()))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onRecordSnapshot));
  }

  private Single<Persistable<Record>> createOrUpdateRecord(EditRecordRequest request) {
    this.isNew = request.isNew;
    return isNew ? newRecord(request) : editRecord(request);
  }

  private Single<Persistable<Record>> newRecord(EditRecordRequest request) {
    return dataRepository
        .createRecord(
            request.args.getProjectId(), request.args.getFeatureId(), request.args.getFormId())
        .map(Persistable::loaded)
        // TODO(#78): Avoid side-effects.
        .doOnSuccess(this::onNewRecordLoaded);
  }

  private Single<Persistable<Record>> editRecord(EditRecordRequest request) {
    return dataRepository
        .getRecord(
            request.args.getProjectId(), request.args.getFeatureId(), request.args.getRecordId())
        // TODO(#78): Avoid side-effects.
        .doOnSuccess(this::update)
        .map(Persistable::loaded);
  }

  private Completable saveRecord(SaveRecordRequest request) {
    RecordMutation recordMutation =
        RecordMutation.builder()
            .setType(request.mutationType)
            .setProjectId(request.record.getProject().getId())
            .setFeatureId(request.record.getFeature().getId())
            .setFeatureTypeId(request.record.getFeature().getFeatureType().getId())
            .setRecordId(request.record.getId())
            .setFormId(request.record.getForm().getId())
            .setResponseDeltas(getResponseDeltas(request.record))
            .setUserId(request.user.getId())
            .build();
    return dataRepository.applyAndEnqueue(recordMutation);
  }

  private void onSaveRecordError(Throwable t) {
    Log.e(TAG, "Failed to save the record.", t);
  }

  private void onEditRecordError(Throwable t) {
    Log.e(TAG, "Unable to create or update record", t);
  }

  public ObservableMap<String, Response> getResponses() {
    return responses;
  }

  public Optional<Response> getResponse(String fieldId) {
    return Optional.ofNullable(responses.get(fieldId));
  }

  public ObservableMap<String, String> getErrors() {
    return errors;
  }

  public void onTextChanged(Field field, String text) {
    Log.v(TAG, "onTextChanged: " + field.getId());

    onResponseChanged(field, TextResponse.fromString(text));
  }

  public void onResponseChanged(Field field, Optional<Response> newResponse) {
    Log.v(
        TAG, "onResponseChanged: " + field.getId() + " = '" + Response.toString(newResponse) + "'");
    newResponse.ifPresentOrElse(
        r -> responses.put(field.getId(), r), () -> responses.remove(field.getId()));
    updateError(field, newResponse);
  }

  public void onFocusChange(Field field, boolean hasFocus) {
    if (!hasFocus) {
      updateError(field);
    }
  }

  LiveData<Persistable<Record>> getRecord() {
    return record;
  }

  LiveData<Void> getShowUnsavedChangesDialogEvents() {
    return showUnsavedChangesDialogEvents;
  }

  public LiveData<Void> getShowErrorDialogEvents() {
    return showErrorDialogEvents;
  }

  @NonNull
  private Optional<Record> getCurrentRecord() {
    return Persistable.getData(record);
  }

  private void onNewRecordLoaded(Persistable<Record> r) {
    responses.clear();
    errors.clear();
  }

  private void updateMap(Record r) {
    Log.v(TAG, "Updating map");
    responses.clear();
    for (String fieldId : r.getResponses().fieldIds()) {
      r.getForm()
          .getField(fieldId)
          .ifPresent(field -> onResponseChanged(field, r.getResponses().getResponse(fieldId)));
    }
  }

  void editRecord(EditRecordFragmentArgs args, boolean isNew) {
    this.currentUser = authManager.getUser().blockingFirst(AuthenticationManager.User.ANONYMOUS);
    // TODO(#100): Replace event object with single value (id?).
    editRecordRequests.onNext(new EditRecordRequest(args, isNew));
  }

  private void onRecordSnapshot(Persistable<Record> r) {
    switch (r.state()) {
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
    // TODO(#100): Replace event object with single value (id?).
    recordSaveRequests.onNext(
        new SaveRecordRequest(
            r, this.currentUser, isNew ? Mutation.Type.CREATE : Mutation.Type.UPDATE));
  }

  private ImmutableList<ResponseDelta> getResponseDeltas(Record record) {
    ImmutableList.Builder<ResponseDelta> deltas = ImmutableList.builder();
    for (Element e : record.getForm().getElements()) {
      if (e.getType() != Type.FIELD) {
        continue;
      }
      String fieldId = e.getField().getId();
      Optional<Response> originalResponse = record.getResponses().getResponse(fieldId);
      Optional<Response> currentResponse = getResponse(fieldId).filter(r -> !r.isEmpty());
      if (!currentResponse.equals(originalResponse)) {
        deltas.add(
            ResponseDelta.builder().setFieldId(fieldId).setNewResponse(currentResponse).build());
      }
    }
    return deltas.build();
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
    updateError(field, getResponse(field.getId()));
  }

  private void updateError(Field field, Optional<Response> response) {
    String key = field.getId();
    if (field.isRequired() && !response.filter(r -> !r.isEmpty()).isPresent()) {
      Log.d(TAG, "Missing: " + key);
      errors.put(field.getId(), resources.getString(R.string.required_field));
    } else {
      Log.d(TAG, "Valid: " + key);
      errors.remove(field.getId());
    }
  }

  private boolean hasUnsavedChanges() {
    return getCurrentRecord().map(record -> !getResponseDeltas(record).isEmpty()).orElse(false);
  }

  private boolean hasErrors() {
    return !errors.isEmpty();
  }

  public static class EditRecordRequest {
    public final EditRecordFragmentArgs args;
    public final boolean isNew;

    EditRecordRequest(EditRecordFragmentArgs args, boolean isNew) {
      this.args = args;
      this.isNew = isNew;
    }
  }

  public static class SaveRecordRequest {
    public final Record record;
    public final AuthenticationManager.User user;
    public final Mutation.Type mutationType;

    SaveRecordRequest(Record record, AuthenticationManager.User user, Mutation.Type mutationType) {
      this.record = record;
      this.user = user;
      this.mutationType = mutationType;
    }
  }
}
