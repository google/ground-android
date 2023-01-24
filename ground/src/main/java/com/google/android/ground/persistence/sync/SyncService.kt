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
package com.google.android.ground.persistence.sync

import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Base class for creating a work manager for scheduling background tasks.
 *
 * By default, the only constraint is availability of any type of internet connection, as it is
 * assumed that all background tasks need at least some sort of connectivity.
 *
 * In case the required criteria are not met, the next attempt uses LINEAR backoff policy with a
 * backoff delay of 10 seconds.
 */
abstract class SyncService {
  /** A set of constraints that must be satisfied in order to start the scheduled job. */
  private val workerConstraints: Constraints
    get() = Constraints.Builder().setRequiredNetworkType(preferredNetworkType()).build()

  /**
   * Override this method if the worker requires a stable internet connection for large file
   * upload/download. By default, the worker just needs access to internet connection.
   */
  protected open fun preferredNetworkType(): NetworkType = DEFAULT_NETWORK_TYPE

  /** A class extending [Worker] which gets scheduled for a request. */
  protected abstract val workerClass: Class<out Worker?>

  /**
   * Create a work request for non-repeating work along with input data that would be passed along
   * to the worker class.
   */
  @JvmOverloads
  protected fun buildWorkerRequest(inputData: Data? = null): OneTimeWorkRequest {
    val builder =
      OneTimeWorkRequest.Builder(workerClass)
        .setConstraints(workerConstraints)
        .setBackoffCriteria(BACKOFF_POLICY, BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)

    if (inputData != null) {
      builder.setInputData(inputData)
    }

    return builder.build()
  }

  companion object {
    /** Backoff time should increase linearly. */
    // TODO(#710): Check if it is possible to wake the worker as soon as the connection becomes
    //   available, and if so switch to EXPONENTIAL backoff policy.
    private val BACKOFF_POLICY = BackoffPolicy.LINEAR

    /** Number of milliseconds to wait before retrying failed sync tasks. */
    private const val BACKOFF_DELAY_MILLIS = WorkRequest.MIN_BACKOFF_MILLIS

    /** Any working network connection is required for this work. */
    private val DEFAULT_NETWORK_TYPE = NetworkType.CONNECTED
  }
}
