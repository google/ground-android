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

import android.view.View;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Persistable;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Record;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;

public class RecordDetailsViewModel extends AbstractViewModel {

  private static final String TAG = RecordDetailsViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final BehaviorProcessor<RecordDetailsFragmentArgs> argsProcessor;
  public final LiveData<Persistable<Record>> records;
  public final LiveData<Integer> progressBarVisibility;
  public final LiveData<String> toolbarTitle;
  public final LiveData<String> toolbarSubtitle;
  public final LiveData<String> formNameView;

  @Inject
  RecordDetailsViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;

    this.argsProcessor = BehaviorProcessor.create();

    Flowable<Persistable<Record>> recordStream =
        argsProcessor
            .switchMapSingle(
                args ->
                    this.dataRepository
                        .getRecord(args.getProjectId(), args.getFeatureId(), args.getRecordId())
                        .map(Persistable::loaded)
                        .onErrorReturn(Persistable::error)
                        .subscribeOn(Schedulers.io()))
            .observeOn(AndroidSchedulers.mainThread());

    // TODO: Refactor to expose the fetched record directly.
    this.records = LiveDataReactiveStreams.fromPublisher(recordStream);

    this.progressBarVisibility =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(RecordDetailsViewModel::getProgressBarVisibility));

    this.toolbarTitle =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(RecordDetailsViewModel::getToolbarTitle));

    this.toolbarSubtitle =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(RecordDetailsViewModel::getToolbarSubtitle));

    this.formNameView =
        LiveDataReactiveStreams.fromPublisher(
            recordStream.map(RecordDetailsViewModel::getFormNameView));
  }

  public void loadRecordDetails(RecordDetailsFragmentArgs args) {
    this.argsProcessor.onNext(args);
  }

  private static Integer getProgressBarVisibility(Persistable<Record> record) {
    return record.data().isPresent() ? View.VISIBLE : View.GONE;
  }

  private static String getToolbarTitle(Persistable<Record> record) {
    return record.data().map(Record::getFeature).map(Feature::getTitle).orElse("");
  }

  private static String getToolbarSubtitle(Persistable<Record> record) {
    return record.data().map(Record::getFeature).map(Feature::getSubtitle).orElse("");
  }

  private static String getFormNameView(Persistable<Record> record) {
    return record.data().map(Record::getForm).map(Form::getTitle).orElse("");
  }
}
