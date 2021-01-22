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

/**
 * Base class for creating a work manager for scheduling background tasks.
 *
 * <p>By default, the only constraint is availability of any type of internet connection, as it is
 * assumed that all background tasks need at least some sort of connectivity.
 *
 * <p>In case the required criteria are not met, the next attempt uses LINEAR backoff policy with a
 * backoff delay of 10 seconds.
 */
public abstract class BaseWorkManager {

  /** Backoff time should increase linearly. */
  // TODO(#710): Check if it is possible to wake the worker as soon as the connection becomes
  //   available, and if so switch to EXPONENTIAL backoff policy.
  private static final BackoffPolicy BACKOFF_POLICY = BackoffPolicy.LINEAR;

  /** Number of milliseconds to wait before retrying failed sync tasks. */
  private static final long BACKOFF_DELAY_MILLIS = WorkRequest.MIN_BACKOFF_MILLIS;

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

  /** A set of constraints that must be satisfied in order to start the scheduled job. */
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

  /** A class extending {@link BaseWorker} which gets scheduled for a request. */
  abstract Class<? extends BaseWorker> getWorkerClass();

  /** Create a work request for non-repeating work with default constraints and backoff-criteria. */
  protected OneTimeWorkRequest buildWorkerRequest() {
    return buildWorkerRequest(null);
  }

  /**
   * Create a work request for non-repeating work along with input data that would be passed along
   * to the worker class.
   */
  protected OneTimeWorkRequest buildWorkerRequest(@Nullable Data inputData) {
    Builder builder =
        new Builder(getWorkerClass())
            .setConstraints(getWorkerConstraints())
            .setBackoffCriteria(BACKOFF_POLICY, BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS);

    if (inputData != null) {
      builder.setInputData(inputData);
    }

    return builder.build();
  }
}
