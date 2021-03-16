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

import android.content.Context;
import android.content.res.Resources;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.R;
import com.google.android.gnd.model.basemap.OfflineBaseMap;
import com.google.android.gnd.model.basemap.OfflineBaseMap.State;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.repository.OfflineBaseMapRepository;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import javax.inject.Inject;
import timber.log.Timber;

public class OfflineBaseMapSelectorViewModel extends AbstractViewModel {

  private final Resources resources;

  enum DownloadMessage {
    STARTED,
    FAILURE
  }

  @Hot private final SingleSubject<OfflineBaseMap> baseMapDownloads = SingleSubject.create();

  @Hot(terminates = true, errors = false)
  private final Single<DownloadMessage> downloadMessage;

  private final OfflineUuidGenerator offlineUuidGenerator;

  @Inject
  OfflineBaseMapSelectorViewModel(
      OfflineBaseMapRepository offlineBaseMapRepository,
      OfflineUuidGenerator offlineUuidGenerator,
      @ApplicationContext Context context) {
    this.downloadMessage =
        baseMapDownloads
            .flatMapCompletable(offlineBaseMapRepository::addAreaAndEnqueue)
            .toSingleDefault(DownloadMessage.STARTED)
            .onErrorReturn(this::onEnqueueError);
    this.offlineUuidGenerator = offlineUuidGenerator;
    this.resources = context.getResources();
  }

  /** Returns a failure message if the basemap download enqueued by this viewmodel fails. */
  private DownloadMessage onEnqueueError(Throwable e) {
    Timber.e("Failed to add area and queue downloads: %s", e.getMessage());
    return DownloadMessage.FAILURE;
  }

  /** The result of attempting to download a basemap; completes with the latest value. */
  @Hot(terminates = true, errors = false)
  public Single<DownloadMessage> getDownloadMessages() {
    return this.downloadMessage;
  }

  // TODO: Use an abstraction over LatLngBounds
  /** Queues a basemap captured in the current map viewport for download. */
  public void downloadBaseMap(LatLngBounds viewport) {
    OfflineBaseMap offlineBaseMap =
        OfflineBaseMap.newBuilder()
            .setBounds(viewport)
            .setId(offlineUuidGenerator.generateUuid())
            .setState(State.PENDING)
            .setName(resources.getString(R.string.unnamed_area))
            .build();

    baseMapDownloads.onSuccess(offlineBaseMap);
  }
}
