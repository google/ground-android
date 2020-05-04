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

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import io.reactivex.Completable;
import javax.inject.Inject;
import javax.inject.Provider;

/** Enqueues file download work to be done in the background. */
public class TileDownloadWorkManager {
  private static final Constraints CONSTRAINTS =
      new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();

  private final Provider<WorkManager> workManagerProvider;

  @Inject
  public TileDownloadWorkManager(Provider<WorkManager> workManagerProvider) {
    this.workManagerProvider = workManagerProvider;
  }

  /**
   * Enqueues a worker that downloads files when a network connection is available, returning a
   * completeable upon enqueueing.
   */
  public Completable enqueueTileDownloadWorker() {
    return Completable.fromRunnable(this::enqueueTileDownloadWorkerInternal);
  }

  private void enqueueTileDownloadWorkerInternal() {
    OneTimeWorkRequest request = buildWorkerRequest();

    getWorkManager().enqueue(request);
  }

  private WorkManager getWorkManager() {
    return workManagerProvider.get().getInstance();
  }

  private OneTimeWorkRequest buildWorkerRequest() {
    return new OneTimeWorkRequest.Builder(TileDownloadWorker.class)
        .setConstraints(CONSTRAINTS)
        .build();
  }
}
