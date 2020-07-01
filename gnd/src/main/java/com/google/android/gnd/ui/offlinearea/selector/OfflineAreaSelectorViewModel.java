/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.ui.offlinearea.selector;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.repository.OfflineAreaRepository;
import com.google.android.gnd.rx.Event;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.ui.common.AbstractViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import javax.inject.Inject;
import timber.log.Timber;

public class OfflineAreaSelectorViewModel extends AbstractViewModel {

  private final LiveData<Event<DownloadEvent>> downloadEvents;

  public LiveData<Event<DownloadEvent>> getDownloadEvents() {
    return this.downloadEvents;
  }

  enum DownloadEvent {
    STARTED,
    FAILURE
  }

  private final PublishSubject<DownloadEvent> downloadsPublishSubject = PublishSubject.create();
  private final PublishSubject<LatLngBounds> downloadClickSubject = PublishSubject.create();

  @Inject
  OfflineAreaSelectorViewModel(OfflineAreaRepository offlineAreaRepository, Schedulers schedulers) {

    disposeOnClear(
        downloadClickSubject
            .flatMap(
                // We need to handle this in the inner lambda instead of using a combinator such as
                // flatMapCompletable because the PublishSubject will never actually complete.
                // andThen enables us to fill the resulting stream w/ items to propagate completion.
                viewport ->
                    offlineAreaRepository
                        .addAreaAndEnqueue(viewport)
                        .andThen(Observable.just(new Object())))
            .observeOn(schedulers.ui())
            .doOnError(e -> Timber.e("Failed to add area and queue downloads: %s", e.getMessage()))
            .subscribe(
                __ -> downloadsPublishSubject.onNext(DownloadEvent.STARTED),
                __ -> downloadsPublishSubject.onNext(DownloadEvent.FAILURE)));

    this.downloadEvents =
        LiveDataReactiveStreams.fromPublisher(
            downloadsPublishSubject.toFlowable(BackpressureStrategy.LATEST).map(Event::create));
  }

  // TODO: Use an abstraction over LatLngBounds
  public void onDownloadClick(LatLngBounds viewport) {
    Timber.d("viewport:%s", viewport);
    downloadClickSubject.onNext(viewport);
  }
}
