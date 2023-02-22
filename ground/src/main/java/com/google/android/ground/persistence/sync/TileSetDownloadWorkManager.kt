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
package com.google.android.ground.persistence.sync

import androidx.work.NetworkType
import androidx.work.WorkManager
import com.google.android.ground.persistence.local.LocalValueStore
import io.reactivex.Completable
import javax.inject.Inject

/** Enqueues file download work to be done in the background. */
class TileSetDownloadWorkManager
@Inject
constructor(private val workManager: WorkManager, private val localValueStore: LocalValueStore) :
  SyncService() {
  override val workerClass: Class<TileSetDownloadWorker>
    get() = TileSetDownloadWorker::class.java

  override val preferredNetworkType: NetworkType =
    if (localValueStore.shouldDownloadOfflineAreasOverUnmeteredConnectionOnly())
      NetworkType.UNMETERED
    else NetworkType.CONNECTED

  /**
   * Enqueues a worker that downloads files when a network connection is available, returning a
   * completable upon enqueueing.
   */
  fun enqueueTileSetDownloadWorker(): Completable =
    Completable.fromRunnable { enqueueTileSetDownloadWorkerInternal() }

  private fun enqueueTileSetDownloadWorkerInternal() {
    workManager.enqueue(buildWorkerRequest())
  }
}
