/*
 * Copyright 2026 Google LLC
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
import android.util.Log
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.data.local.LocalValueStore
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Stand-in for [MediaUploadWorker], which can't be instantiated without Hilt's worker factory. */
private class NoOpWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
  override fun doWork(): Result = Result.success()
}

/**
 * Tests media upload scheduling against a real [WorkManager] so that the constraints of enqueued
 * requests, and the dependencies between them, can be asserted.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MediaUploadWorkManagerTest : BaseHiltTest() {
  @Inject lateinit var localValueStore: LocalValueStore

  private lateinit var workManager: WorkManager
  private lateinit var mediaUploadWorkManager: MediaUploadWorkManager

  @Before
  override fun setUp() {
    super.setUp()
    workManager = createTestWorkManager()
    mediaUploadWorkManager = MediaUploadWorkManager(workManager, localValueStore)
  }

  @Test
  fun `enqueueSyncWorker() requires any connection when wifi only uploads are disabled`() =
    runWithTestDispatcher {
      localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = false

      mediaUploadWorkManager.enqueueSyncWorker()

      assertThat(pendingNetworkTypes()).containsExactly(NetworkType.CONNECTED)
    }

  @Test
  fun `enqueueSyncWorker() requires an unmetered connection when wifi only uploads are enabled`() =
    runWithTestDispatcher {
      localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = true

      mediaUploadWorkManager.enqueueSyncWorker()

      assertThat(pendingNetworkTypes()).containsExactly(NetworkType.UNMETERED)
    }

  @Test
  fun `enqueueSyncWorker() chains onto existing work rather than discarding it`() =
    runWithTestDispatcher {
      localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = true

      mediaUploadWorkManager.enqueueSyncWorker()
      mediaUploadWorkManager.enqueueSyncWorker()

      assertThat(pendingWorkInfos().map { it.state })
        .containsExactly(WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED)
    }

  @Test
  fun `rescheduleSyncWorker() stops requiring an unmetered connection`() = runWithTestDispatcher {
    localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = true
    mediaUploadWorkManager.enqueueSyncWorker()

    localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = false
    mediaUploadWorkManager.rescheduleSyncWorker()

    assertThat(pendingNetworkTypes()).containsExactly(NetworkType.CONNECTED)
  }

  @Test
  fun `rescheduleSyncWorker() starts requiring an unmetered connection`() = runWithTestDispatcher {
    localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = false
    mediaUploadWorkManager.enqueueSyncWorker()

    localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = true
    mediaUploadWorkManager.rescheduleSyncWorker()

    assertThat(pendingNetworkTypes()).containsExactly(NetworkType.UNMETERED)
  }

  @Test
  fun `rescheduleSyncWorker() discards work blocked on an unmetered connection`() =
    runWithTestDispatcher {
      localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = true
      // Enqueue twice so that the second request is chained behind the first, blocked one.
      mediaUploadWorkManager.enqueueSyncWorker()
      mediaUploadWorkManager.enqueueSyncWorker()
      assertThat(pendingWorkInfos()).hasSize(2)

      localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = false
      mediaUploadWorkManager.rescheduleSyncWorker()

      // The stale chain is discarded, leaving a single runnable request.
      assertThat(allWorkInfos()).hasSize(1)
      assertThat(pendingWorkInfos().map { it.state }).containsExactly(WorkInfo.State.ENQUEUED)
      assertThat(pendingNetworkTypes()).containsExactly(NetworkType.CONNECTED)
    }

  @Test
  fun `rescheduleSyncWorker() enqueues a single request when nothing is pending`() =
    runWithTestDispatcher {
      localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = false

      mediaUploadWorkManager.rescheduleSyncWorker()

      assertThat(pendingWorkInfos().map { it.state }).containsExactly(WorkInfo.State.ENQUEUED)
      assertThat(pendingNetworkTypes()).containsExactly(NetworkType.CONNECTED)
    }

  @Test
  fun `cancelSyncWorker() cancels pending work`() = runWithTestDispatcher {
    localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = true
    mediaUploadWorkManager.enqueueSyncWorker()

    mediaUploadWorkManager.cancelSyncWorker()

    assertThat(pendingWorkInfos()).isEmpty()
  }

  @Test
  fun `enqueueSyncWorker() isn't blocked by work cancelled by cancelSyncWorker()`() =
    runWithTestDispatcher {
      // Work left behind awaiting an unmetered connection, e.g. by the previously signed in user.
      localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = true
      mediaUploadWorkManager.enqueueSyncWorker()
      mediaUploadWorkManager.cancelSyncWorker()

      localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = false
      mediaUploadWorkManager.enqueueSyncWorker()

      assertThat(pendingWorkInfos().map { it.state }).containsExactly(WorkInfo.State.ENQUEUED)
      assertThat(pendingNetworkTypes()).containsExactly(NetworkType.CONNECTED)
    }

  private suspend fun allWorkInfos(): List<WorkInfo> =
    workManager.getWorkInfosForUniqueWork(MediaUploadWorker::class.java.name).await()

  private suspend fun pendingWorkInfos(): List<WorkInfo> =
    allWorkInfos().filter { !it.state.isFinished }

  private suspend fun pendingNetworkTypes(): List<NetworkType> =
    pendingWorkInfos().map { it.constraints.requiredNetworkType }
}

private fun createTestWorkManager(): WorkManager {
  val context = ApplicationProvider.getApplicationContext<Context>()
  val config =
    Configuration.Builder()
      .setMinimumLoggingLevel(Log.VERBOSE)
      .setExecutor(SynchronousExecutor())
      .setWorkerFactory(
        object : WorkerFactory() {
          override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
          ) = NoOpWorker(appContext, workerParameters)
        }
      )
      .build()
  WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
  return WorkManager.getInstance(context)
}
