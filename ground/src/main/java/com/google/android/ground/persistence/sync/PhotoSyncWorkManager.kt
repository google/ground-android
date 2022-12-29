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

import androidx.work.NetworkType
import androidx.work.WorkManager
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.sync.PhotoSyncWorker.Companion.createInputData
import com.google.android.ground.repository.UserMediaRepository
import javax.inject.Inject
import timber.log.Timber

/** Enqueues photo upload work to be done in the background. */
class PhotoSyncWorkManager
@Inject
constructor(
  private val workManager: WorkManager,
  private val localValueStore: LocalValueStore,
  private val userMediaRepository: UserMediaRepository
) : BaseWorkManager() {
  override val workerClass: Class<PhotoSyncWorker>
    get() = PhotoSyncWorker::class.java

  override fun preferredNetworkType(): NetworkType =
    if (localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly()) NetworkType.UNMETERED
    else NetworkType.CONNECTED

  /**
   * Enqueues a worker that uploads selected/captured photo to the remote FirestoreStorage once a
   * network connection is available. The returned `Completable` completes immediately as soon as
   * the worker is added to the work queue (not once the sync job completes).
   */
  fun enqueueSyncWorker(remotePath: String) {
    val localFile = userMediaRepository.getLocalFileFromRemotePath(remotePath)
    if (!localFile.exists()) {
      Timber.e("Local file not found: %s", localFile.path)
      return
    }
    val inputData = createInputData(localFile.path, remotePath)
    val request = buildWorkerRequest(inputData)
    workManager.enqueue(request)
  }
}
