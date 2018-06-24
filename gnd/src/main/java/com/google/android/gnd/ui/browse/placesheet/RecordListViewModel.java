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

package com.google.android.gnd.ui.browse.placesheet;

import static java8.util.stream.StreamSupport.stream;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import com.google.android.gnd.repository.GndDataRepository;
import com.google.android.gnd.repository.ProjectState;
import com.google.android.gnd.repository.RecordSummary;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.PlaceType;
import com.google.android.gnd.vo.Project;
import io.reactivex.Completable;
import java.util.Collections;
import java.util.List;
import java8.util.Optional;
import java8.util.stream.Collectors;
import javax.inject.Inject;

public class RecordListViewModel extends ViewModel {
  private static final String TAG = RecordListViewModel.class.getSimpleName();
  private final GndDataRepository dataRepository;
  private MutableLiveData<List<RecordSummary>> recordSummaries;

  @Inject
  public RecordListViewModel(GndDataRepository dataRepository) {
    this.dataRepository = dataRepository;
    recordSummaries = new MutableLiveData<>();
  }

  public LiveData<List<RecordSummary>> getRecords() {
    return recordSummaries;
  }

  public void clearRecords() {
    recordSummaries.setValue(Collections.emptyList());
  }

  public Completable loadRecords(Place place, Form form) {
    PlaceType placeType = place.getPlaceType();
    return dataRepository
        .getProjectState()
        .map(ProjectState::getActiveProject)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMapCompletable(
            project -> loadRecords(project, placeType.getId(), form.getId(), place.getId()));
  }

  private Completable loadRecords(
      Project project, String placeTypeId, String formId, String placeId) {
    Optional<Form> form = project.getPlaceType(placeTypeId).flatMap(pt -> pt.getForm(formId));
    if (!form.isPresent()) {
      return Completable.error(new IllegalArgumentException("Form " + formId + " not found!"));
    }
    return dataRepository
        .loadRecordSummaries(project, placeId)
        .doOnSuccess(
            // TODO: Only fetch records w/current formId.
            records ->
                recordSummaries.setValue(
                    stream(records)
                        .filter(record -> record.getFormId().equals(formId))
                        .map(record -> new RecordSummary(form.get(), record))
                        .collect(Collectors.toList())))
        .toCompletable();
  }
}
