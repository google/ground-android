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

package com.google.android.gnd.ui.editobservation;

import static java8.util.stream.StreamSupport.stream;

import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableField;
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
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Observation;
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
import io.reactivex.subjects.PublishSubject;
import java8.util.Optional;
import javax.inject.Inject;

// TODO: Save draft to local db on each change.
public class EditObservationViewModel extends AbstractViewModel {
  private static final String TAG = EditObservationViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final AuthenticationManager authManager;
  private final MutableLiveData<Persistable<Observation>> record;
  private final SingleLiveEvent<Void> showUnsavedChangesDialogEvents;
  private final SingleLiveEvent<Void> showErrorDialogEvents;
  private final Resources resources;
  private final ObservableMap<String, Response> responses = new ObservableArrayMap<>();
  private final ObservableMap<String, String> errors = new ObservableArrayMap<>();
  private final PublishSubject<EditRecordRequest> editRecordRequests;
  private final PublishSubject<SaveRecordRequest> recordSaveRequests;

  public final ObservableField<String> formNameView = new ObservableField<>();
  public final ObservableInt loadingSpinnerVisibility = new ObservableInt();
  public final ObservableInt saveButtonVisibility = new ObservableInt(View.GONE);
  private AuthenticationManager.User currentUser;
  private boolean isNew;

  @Inject
  EditObservationViewModel(
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
                        .startWith(Persistable.saving(request.observation))
                        .map(__ -> Persistable.saved(request.observation))
                        .doOnError(this::onSaveRecordError)
                        // Prevent from breaking upstream.
                        .onErrorResumeNext(Observable.never()))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(record::setValue));

    disposeOnClear(
        editRecordRequests
            .switchMapSingle(
                record ->
                    createOrUpdateRecord(record)
                        .doOnError(this::onEditRecordError)
                        .onErrorResumeNext(Single.never()))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onRecordSnapshot));
  }

  private String getFormNameView(Persistable<Observation> record) {
    return record.value().map(Observation::getForm).map(Form::getTitle).orElse("");
  }

  private Single<Persistable<Observation>> createOrUpdateRecord(EditRecordRequest request) {
    this.isNew = request.isNew;
    return isNew ? newRecord(request) : editRecord(request);
  }

  private Single<Persistable<Observation>> newRecord(EditRecordRequest request) {
    return dataRepository
        .createRecord(
            request.args.getProjectId(), request.args.getFeatureId(), request.args.getFormId())
        .map(Persistable::loaded)
        // TODO(#78): Avoid side-effects.
        .doOnSuccess(this::onNewRecordLoaded);
  }

  private Single<Persistable<Observation>> editRecord(EditRecordRequest request) {
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
            .setProjectId(request.observation.getProject().getId())
            .setFeatureId(request.observation.getFeature().getId())
            .setLayerId(request.observation.getFeature().getLayer().getId())
            .setRecordId(request.observation.getId())
            .setFormId(request.observation.getForm().getId())
            .setResponseDeltas(getResponseDeltas(request.observation))
            .setUserId(request.user.getId())
            .build();
    return dataRepository.applyAndEnqueue(recordMutation);
  }

  private void onSaveRecordError(Throwable t) {
    Log.e(TAG, "Failed to save the observation.", t);
  }

  private void onEditRecordError(Throwable t) {
    Log.e(TAG, "Unable to create or update observation", t);
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

  LiveData<Persistable<Observation>> getRecord() {
    return record;
  }

  LiveData<Void> getShowUnsavedChangesDialogEvents() {
    return showUnsavedChangesDialogEvents;
  }

  public LiveData<Void> getShowErrorDialogEvents() {
    return showErrorDialogEvents;
  }

  @NonNull
  private Optional<Observation> getCurrentRecord() {
    return Persistable.getData(record);
  }

  private void onNewRecordLoaded(Persistable<Observation> r) {
    responses.clear();
    errors.clear();
  }

  private void updateMap(Observation r) {
    Log.v(TAG, "Updating map");
    responses.clear();
    for (String fieldId : r.getResponses().fieldIds()) {
      r.getForm()
          .getField(fieldId)
          .ifPresent(field -> onResponseChanged(field, r.getResponses().getResponse(fieldId)));
    }
  }

  void editRecord(EditObservationFragmentArgs args, boolean isNew) {
    this.currentUser = authManager.getUser().blockingFirst(AuthenticationManager.User.ANONYMOUS);
    // TODO(#100): Replace event object with single value (id?).
    editRecordRequests.onNext(new EditRecordRequest(args, isNew));
  }

  private void onRecordSnapshot(Persistable<Observation> r) {
    switch (r.state()) {
      case LOADING:
        saveButtonVisibility.set(View.GONE);
        loadingSpinnerVisibility.set(View.VISIBLE);
        break;
      case LOADED:
        saveButtonVisibility.set(View.VISIBLE);
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
    formNameView.set(getFormNameView(r));
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

  private void saveChanges(Observation r) {
    // TODO(#100): Replace event object with single value (id?).
    recordSaveRequests.onNext(
        new SaveRecordRequest(
            r, this.currentUser, isNew ? Mutation.Type.CREATE : Mutation.Type.UPDATE));
  }

  private ImmutableList<ResponseDelta> getResponseDeltas(Observation observation) {
    ImmutableList.Builder<ResponseDelta> deltas = ImmutableList.builder();
    for (Element e : observation.getForm().getElements()) {
      if (e.getType() != Type.FIELD) {
        continue;
      }
      String fieldId = e.getField().getId();
      Optional<Response> originalResponse = observation.getResponses().getResponse(fieldId);
      Optional<Response> currentResponse = getResponse(fieldId).filter(r -> !r.isEmpty());
      if (!currentResponse.equals(originalResponse)) {
        deltas.add(
            ResponseDelta.builder().setFieldId(fieldId).setNewResponse(currentResponse).build());
      }
    }
    return deltas.build();
  }

  private void update(Observation observation) {
    updateMap(observation);
    updateErrors(observation);
  }

  private void updateErrors(Observation r) {
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
    public final EditObservationFragmentArgs args;
    public final boolean isNew;

    EditRecordRequest(EditObservationFragmentArgs args, boolean isNew) {
      this.args = args;
      this.isNew = isNew;
    }
  }

  public static class SaveRecordRequest {
    public final Observation observation;
    public final AuthenticationManager.User user;
    public final Mutation.Type mutationType;

    SaveRecordRequest(Observation observation, AuthenticationManager.User user, Mutation.Type mutationType) {
      this.observation = observation;
      this.user = user;
      this.mutationType = mutationType;
    }
  }
}
