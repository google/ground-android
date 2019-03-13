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
import android.util.Log;
import android.view.View;

import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Record;
import com.google.common.collect.ImmutableList;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import javax.inject.Inject;

public class RecordDetailsViewModel extends AbstractViewModel {

  private static final String TAG = RecordDetailsViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final Subject<RecordDetailsFragmentArgs> recordDetailsRequest;
  public final Flowable<Resource<Record>> recordStream;
  public final LiveData<Resource<Record>> records;
  public final LiveData<Integer> progressBarVisibility;
  public final LiveData<String> toolbarTitle;
  public final LiveData<String> toolbarSubtitle;
  public final LiveData<String> formNameView;

  @Inject
  RecordDetailsViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;

    this.recordDetailsRequest = BehaviorSubject.create();

    this.recordStream =
        recordDetailsRequest
          .switchMap(a ->
              this.dataRepository.getRecordDetails(a.getProjectId(), a.getFeatureId(), a.getRecordId()))
          .toFlowable(BackpressureStrategy.LATEST);

    //TODO: Refactor to expose the fetched record directly.
    this.records = LiveDataReactiveStreams.fromPublisher(
        recordStream
    );

    this.progressBarVisibility = LiveDataReactiveStreams.fromPublisher(
        recordStream.map(this::toProgressBarVisibility)
        .onErrorReturn(x -> View.GONE)
    );

    this.toolbarTitle = LiveDataReactiveStreams.fromPublisher(
        recordStream.map(this::toToolbarTitle)
        .onErrorReturn(x -> "")
    );

    this.toolbarSubtitle = LiveDataReactiveStreams.fromPublisher(
        recordStream.map(this::toToolbarSubtitle)
        .onErrorReturn(x -> "")
    );

    this.formNameView = LiveDataReactiveStreams.fromPublisher(
        recordStream.map(this::toFormNameView)
        .onErrorReturn(x -> "")
    );

    recordDetailsRequest.subscribe(args -> {
      Log.d(TAG, "" + args);
    });
  }

  public void getRecordDetailsRequest(RecordDetailsFragmentArgs args) {
    this.recordDetailsRequest.onNext(args);
  }

  private Integer toProgressBarVisibility(Resource<Record> record) {
    switch (record.operationState().get()) {
      case LOADING:
        return View.VISIBLE;
      default:
        return View.GONE;
    }
  }

  private String toToolbarTitle(Resource<Record> record) {
    switch (record.operationState().get()) {
      case LOADED:
        return record.data()
          .map(r -> r.getFeature().getTitle())
          .orElse("");
      default:
        return "";
    }
  }

  private String toToolbarSubtitle(Resource<Record> record) {
    switch (record.operationState().get()) {
      case LOADED:
        return
          record.data()
            .map(r -> r.getFeature().getSubtitle())
            .orElse("");
      default:
        return "";
    }
  }

  private String toFormNameView(Resource<Record> record) {
    switch (record.operationState().get()) {
      case LOADED:
        return
          record.data()
            .map(r -> r.getForm().getTitle())
            .orElse("");
      default:
        return "";
    }
  }
}
