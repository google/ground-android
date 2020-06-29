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

package com.google.android.gnd.persistence.sync;

import androidx.annotation.Nullable;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OneTimeWorkRequest.Builder;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;

public abstract class BaseWorkManager {

  /** Number of milliseconds to wait before retrying failed sync tasks. */
  private static final long SYNC_BACKOFF_MILLIS = WorkRequest.MIN_BACKOFF_MILLIS;

  /** Any working network connection is required for this work. */
  private static final NetworkType DEFAULT_NETWORK_TYPE = NetworkType.CONNECTED;

  /**
   * WorkManager is injected via {@code Provider} rather than directly to ensure the {@code
   * Application} has a change to initialize it before {@code WorkManager.getInstance()} is called.
   */
  private final Provider<WorkManager> workManagerProvider;

  public BaseWorkManager(Provider<WorkManager> workManagerProvider) {
    this.workManagerProvider = workManagerProvider;
  }

  protected WorkManager getWorkManager() {
    return workManagerProvider.get().getInstance();
  }

  protected Constraints getWorkerConstraints() {
    return new Constraints.Builder().setRequiredNetworkType(preferredNetworkType()).build();
  }

  /**
   * Override this method if the worker requires a stable internet connection for large file
   * upload/download. By default, the worker just needs access to internet connection.
   */
  protected NetworkType preferredNetworkType() {
    return DEFAULT_NETWORK_TYPE;
  }

  abstract Class<? extends BaseWorker> getWorkerClass();

  protected OneTimeWorkRequest buildWorkerRequest(@Nullable Data inputData) {
    Builder builder =
        new Builder(getWorkerClass())
            .setConstraints(getWorkerConstraints())
            .setBackoffCriteria(BackoffPolicy.LINEAR, SYNC_BACKOFF_MILLIS, TimeUnit.MILLISECONDS);

    if (inputData != null) {
      builder.setInputData(inputData);
    }

    return builder.build();
  }
}
