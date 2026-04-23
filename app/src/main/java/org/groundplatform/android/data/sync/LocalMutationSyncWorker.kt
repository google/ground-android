/*
 * Copyright 2022 Google LLC
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

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result.retry
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.groundplatform.android.di.coroutines.IoDispatcher
import org.groundplatform.domain.repository.MutationRepositoryInterface
import timber.log.Timber

/**
 * A worker that uploads all pending local changes to the remote data store. Larger uploads (photos)
 * are then delegated to [MediaUploadWorkManager], which is enqueued and run in parallel.
 */
@HiltWorker
class LocalMutationSyncWorker
@AssistedInject
constructor(
  @Assisted context: Context,
  @Assisted params: WorkerParameters,
  private val mutationRepository: MutationRepositoryInterface,
  private val mediaUploadWorkManager: MediaUploadWorkManager,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result =
    withContext(ioDispatcher) {
      val queue = mutationRepository.getIncompleteUploads()
      Timber.d("Uploading ${queue.size} additions / changes")
      val results = queue.map { mutationRepository.processMutations(it.mutations()) }

      val successfulMutations =
        results.filterIsInstance<MutationRepositoryInterface.MutationResult.Success>()

      if (successfulMutations.any { it.hasPendingMediaUploads }) {
        mediaUploadWorkManager.enqueueSyncWorker()
      }

      if (results.size == successfulMutations.size) success() else retry()
    }
}
