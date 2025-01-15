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

import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkManager
import com.google.android.ground.persistence.local.LocalValueStore
import javax.inject.Inject

/** Enqueues media upload work to be performed in the background. */
class MediaUploadWorkManager
@Inject
constructor(private val workManager: WorkManager, private val localValueStore: LocalValueStore) {

  private fun preferredNetworkType(): NetworkType =
    if (localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly()) NetworkType.UNMETERED
    else NetworkType.CONNECTED

  /**
   * Enqueues a worker that uploads media associated with submissions on a location of interest to
   * the remote storage once a network connection is available.
   *
   * This method returns as soon as the worker is added to the work queue, not when the work
   * completes.
   */
  fun enqueueSyncWorker() {
    val request =
      WorkRequestBuilder()
        .setWorkerClass(MediaUploadWorker::class.java)
        .setNetworkType(preferredNetworkType())
        .buildWorkerRequest()
    workManager.enqueueUniqueWork(
      MediaUploadWorker::class.java.name,
      ExistingWorkPolicy.APPEND_OR_REPLACE,
      request,
    )
  }
}
