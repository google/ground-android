/*
 * Copyright 2019 Google LLC
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

package com.google.android.gnd.persistence.sync;

import androidx.annotation.NonNull;
import androidx.work.NetworkType;
import androidx.work.WorkManager;
import com.google.android.gnd.persistence.local.LocalValueStore;
import io.reactivex.Completable;
import javax.inject.Inject;
import javax.inject.Provider;

/** Enqueues file download work to be done in the background. */
public class TileSourceDownloadWorkManager extends BaseWorkManager {

  private final LocalValueStore localValueStore;

  @Inject
  public TileSourceDownloadWorkManager(
      Provider<WorkManager> workManagerProvider, LocalValueStore localValueStore) {
    super(workManagerProvider);
    this.localValueStore = localValueStore;
  }

  @NonNull
  @Override
  Class<TileSourceDownloadWorker> getWorkerClass() {
    return TileSourceDownloadWorker.class;
  }

  @NonNull
  @Override
  protected NetworkType preferredNetworkType() {
    return localValueStore.shouldDownloadOfflineAreasOverUnmeteredConnectionOnly()
        ? NetworkType.UNMETERED
        : NetworkType.CONNECTED;
  }

  /**
   * Enqueues a worker that downloads files when a network connection is available, returning a
   * completable upon enqueueing.
   */
  @NonNull
  public Completable enqueueTileSourceDownloadWorker() {
    return Completable.fromRunnable(this::enqueueTileSourceDownloadWorkerInternal);
  }

  private void enqueueTileSourceDownloadWorkerInternal() {
    getWorkManager().enqueue(buildWorkerRequest());
  }
}
