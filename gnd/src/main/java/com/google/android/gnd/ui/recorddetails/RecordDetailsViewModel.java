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
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Record;
import com.google.common.collect.ImmutableList;

import org.reactivestreams.Processor;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import javax.inject.Inject;

public class RecordDetailsViewModel extends AbstractViewModel {

  private static final String TAG = RecordDetailsViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final BehaviorProcessor<RecordDetailsFragmentArgs> argsProcessor;
  public final LiveData<Resource<Record>> records;
  public final LiveData<Integer> progressBarVisibility;
  public final LiveData<String> toolbarTitle;
  public final LiveData<String> toolbarSubtitle;
  public final LiveData<String> formNameView;

  @Inject
  RecordDetailsViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;

    this.argsProcessor = BehaviorProcessor.create();

    Flowable<Resource<Record>> recordStream =
        argsProcessor.switchMap(
            args ->
                this.dataRepository.getRecordDetails(
                    args.getProjectId(), args.getFeatureId(), args.getRecordId()));

    // TODO: Refactor to expose the fetched record directly.
    this.records = LiveDataReactiveStreams.fromPublisher(recordStream);

    this.progressBarVisibility =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(this::toProgressBarVisibility).onErrorReturn(x -> View.GONE));

    this.toolbarTitle =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(this::toToolbarTitle).onErrorReturn(x -> ""));

    this.toolbarSubtitle =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(this::toToolbarSubtitle).onErrorReturn(x -> ""));

    this.formNameView =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(this::toFormNameView).onErrorReturn(x -> ""));
  }

  public void loadRecordDetails(RecordDetailsFragmentArgs args) {
    this.argsProcessor.onNext(args);
  }

  private Integer toProgressBarVisibility(Resource<Record> record) {
    return record.data().isPresent() ? View.VISIBLE : View.GONE;
  }

  private String toToolbarTitle(Resource<Record> record) {
    return record.data().map(Record::getFeature).map(Feature::getTitle).orElse("");
  }

  private String toToolbarSubtitle(Resource<Record> record) {
    return record.data().map(Record::getFeature).map(Feature::getSubtitle).orElse("");
  }

  private String toFormNameView(Resource<Record> record) {
    return record.data().map(Record::getForm).map(Form::getTitle).orElse("");
  }
}
