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
package org.groundplatform.android.data.sync

import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkManager
import javax.inject.Inject
import org.groundplatform.android.data.local.LocalValueStore

/** Enqueues media upload work to be performed in the background. */
class MediaUploadWorkManager
@Inject
constructor(private val workManager: WorkManager, private val localValueStore: LocalValueStore) {

  /**
   * Enqueues a worker that uploads media associated with submissions on a location of interest to
   * the remote storage once a network connection is available.
   *
   * The worker is appended to any media upload work which is already pending, so that work which is
   * already running is never interrupted. Use [rescheduleSyncWorker] instead when pending work must
   * pick up a change to the user's preferred network type.
   *
   * This method returns as soon as the worker is added to the work queue, not when the work
   * completes.
   */
  fun enqueueSyncWorker() {
    enqueue(ExistingWorkPolicy.APPEND_OR_REPLACE)
  }

  /**
   * Discards any previously enqueued media upload work and enqueues a new worker which uses the
   * network constraints currently configured by the user.
   *
   * WorkManager constraints are immutable once a request is enqueued, so pending work must be
   * replaced rather than appended to for a change in the user's preferred network type to take
   * effect. Appending would also leave the new request blocked behind work which can no longer run,
   * e.g. work awaiting an unmetered connection which the user no longer wants to wait for. See
   * https://github.com/google/ground-android/issues/3945.
   *
   * Note that this interrupts an upload which is already in progress. The affected mutations remain
   * in the upload queue and are retried by the newly enqueued worker. Enqueuing a worker when
   * nothing is pending is a no-op, since the worker exits immediately when the queue is empty.
   */
  fun rescheduleSyncWorker() {
    enqueue(ExistingWorkPolicy.REPLACE)
  }

  /**
   * Cancels all pending media upload work.
   *
   * Used when local data is cleared, e.g. on sign-out, so that work scheduled for the previous user
   * doesn't linger. Such work may be waiting on constraints which no longer reflect the current
   * user's preferences, in which case it would block subsequently appended uploads indefinitely.
   */
  fun cancelSyncWorker() {
    workManager.cancelUniqueWork(MediaUploadWorker::class.java.name)
  }

  private fun enqueue(existingWorkPolicy: ExistingWorkPolicy) {
    val request =
      WorkRequestBuilder()
        .setWorkerClass(MediaUploadWorker::class.java)
        .setNetworkType(preferredNetworkType())
        .buildWorkerRequest()
    workManager.enqueueUniqueWork(MediaUploadWorker::class.java.name, existingWorkPolicy, request)
  }

  private fun preferredNetworkType(): NetworkType =
    if (localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly) NetworkType.UNMETERED
    else NetworkType.CONNECTED
}
