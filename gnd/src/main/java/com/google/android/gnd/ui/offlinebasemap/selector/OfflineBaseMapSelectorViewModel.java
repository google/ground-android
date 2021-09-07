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

package com.google.android.gnd.ui.offlinebasemap.selector;

import android.content.res.Resources;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.R;
import com.google.android.gnd.model.basemap.OfflineBaseMap;
import com.google.android.gnd.model.basemap.OfflineBaseMap.State;
import com.google.android.gnd.model.basemap.tile.TileSource;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.repository.OfflineBaseMapRepository;
import com.google.android.gnd.rx.Event;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import javax.inject.Inject;
import timber.log.Timber;

public class OfflineBaseMapSelectorViewModel extends AbstractViewModel {

  enum DownloadMessage {
    STARTED,
    FAILURE
  }

  @Hot private final FlowableProcessor<OfflineBaseMap> downloadClicks = PublishProcessor.create();
  @Hot private final FlowableProcessor<Nil> remoteTileRequests = PublishProcessor.create();
  private final LiveData<Event<DownloadMessage>> messages;
  private final Flowable<ImmutableList<TileSource>> remoteTileSources;
  private final OfflineUuidGenerator offlineUuidGenerator;
  @Nullable private LatLngBounds viewport;
  private final Resources resources;

  @Inject
  OfflineBaseMapSelectorViewModel(
      OfflineBaseMapRepository offlineBaseMapRepository,
      OfflineUuidGenerator offlineUuidGenerator,
      Resources resources) {
    this.messages =
        LiveDataReactiveStreams.fromPublisher(
            downloadClicks.switchMapSingle(
                baseMap ->
                    offlineBaseMapRepository
                        .addAreaAndEnqueue(baseMap)
                        .toSingleDefault(DownloadMessage.STARTED)
                        .onErrorReturn(this::onEnqueueError)
                        .map(Event::create)));
    this.offlineUuidGenerator = offlineUuidGenerator;
    this.resources = resources;
    this.remoteTileSources =
            remoteTileRequests.switchMapSingle(
                __ -> offlineBaseMapRepository.getTileSources());
  }

  private DownloadMessage onEnqueueError(Throwable e) {
    Timber.e("Failed to add area and queue downloads: %s", e.getMessage());
    return DownloadMessage.FAILURE;
  }

  public LiveData<Event<DownloadMessage>> getDownloadMessages() {
    return this.messages;
  }

  void setViewport(LatLngBounds viewport) {
    this.viewport = viewport;
  }

  public void onDownloadClick() {
    Timber.d("viewport:%s", viewport);
    if (viewport == null) {
      return;
    }

    downloadClicks.onNext(
        OfflineBaseMap.newBuilder()
            .setBounds(viewport)
            .setId(offlineUuidGenerator.generateUuid())
            .setState(State.PENDING)
            .setName(resources.getString(R.string.unnamed_area))
            .build());
  }

  public Flowable<ImmutableList<TileSource>> getRemoteTileSources() {
    return this.remoteTileSources;
  }

  public void requestRemoteTileSources() {
    remoteTileRequests.onNext(Nil.NIL);
  }
}
