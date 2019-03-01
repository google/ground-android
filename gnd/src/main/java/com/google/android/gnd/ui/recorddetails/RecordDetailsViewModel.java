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

package com.google.android.gnd.ui.recorddetails;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.LiveDataReactiveStreams;
import android.view.View;

import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.Record;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;

import javax.inject.Inject;

public class RecordDetailsViewModel extends AbstractViewModel {

  private final DataRepository dataRepository;
  private LiveData<Resource<Record>> record;
  private LiveData<Integer> progressBarVisibility;

  @Inject
  RecordDetailsViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  public LiveData<Resource<Record>> getRecord() {
    return record;
  }

  public LiveData<Integer> getProgressBarVisibility() {
    return progressBarVisibility;
  }

  public void loadRecordDetails(String projectId, String featureId, String recordId) {
    Observable<Resource<Record>> recordDetials =
      dataRepository.getRecordDetails(projectId, featureId, recordId);

    progressBarVisibility =
      LiveDataReactiveStreams.fromPublisher(
        recordDetials
          .map(this::toProgressBarVisibility)
          .onErrorReturn(err -> View.GONE)
          .toFlowable(BackpressureStrategy.LATEST));

    record =
      LiveDataReactiveStreams.fromPublisher(
        recordDetials
          .map(r -> r)
          .onErrorReturn(err -> Resource.error(err))
          .toFlowable(BackpressureStrategy.LATEST));
  }

  private Integer toProgressBarVisibility(Resource<Record> r) {
    if(r.operationState().get() == Resource.Status.LOADING) {
      return View.VISIBLE;
    } else {
      return View.GONE;
    }
  }
}
