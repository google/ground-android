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

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import io.reactivex.Completable;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/** Enqueues data sync work to be done in the background. */
public class DataSyncWorkManager {
  private static final long SYNC_RETRY_BACKOFF_SECS = 5;
  private final WorkManager workManager;

  @Inject
  public DataSyncWorkManager() {
    // TODO: Inject WorkManager to allow stubbing in unit tests.
    this.workManager = WorkManager.getInstance();
  }

  /**
   * Enqueues a worker that sends changes made locally to the remote data store once a network
   * connection is available. The returned {@code Completable} completes immediately as soon as the
   * worker is added to the work queue (not once the sync job completes).
   */
  public Completable enqueueSyncWorker() {
    return Completable.fromRunnable(this::enqueueSyncWorkerInternal);
  }

  private void enqueueSyncWorkerInternal() {
    // Rather than have running workers monitor the queue for new mutations, we instead queue a
    // new worker on new mutations. This simplified worker implementation and avoids race conditions
    // in case the worker finishes just when new mutations are added to the db.
    workManager.enqueueUniqueWork(
        LocalMutationSyncWorker.class.getName(), ExistingWorkPolicy.APPEND, buildWorkerRequest());
  }

  private Constraints getWorkerConstraints() {
    // TODO: Make required NetworkType configurable.
    return new Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_ROAMING).build();
  }

  private OneTimeWorkRequest buildWorkerRequest() {
    return new OneTimeWorkRequest.Builder(LocalMutationSyncWorker.class)
        .setConstraints(getWorkerConstraints())
        .setBackoffCriteria(BackoffPolicy.LINEAR, SYNC_RETRY_BACKOFF_SECS, TimeUnit.SECONDS)
        .build();
  }
}
