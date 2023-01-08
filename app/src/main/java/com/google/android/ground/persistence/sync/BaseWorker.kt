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

import android.app.Notification
import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.ground.persistence.remote.TransferProgress
import com.google.android.ground.system.NotificationManager
import io.reactivex.Completable
import io.reactivex.Flowable

abstract class BaseWorker
internal constructor(
  context: Context,
  workerParams: WorkerParameters,
  private val notificationManager: NotificationManager,
  private val notificationId: Int
) : Worker(context, workerParams) {
  /** Content text displayed in the notification. */
  abstract val notificationTitle: String

  fun <T> notifyTransferState(upstream: Flowable<T>): Flowable<T> =
    upstream
      .doOnSubscribe { sendNotification(TransferProgress.starting()) }
      .doOnError { sendNotification(TransferProgress.failed()) }
      .doOnComplete { sendNotification(TransferProgress.completed()) }

  fun notifyTransferState(completable: Completable): Completable =
    completable
      .doOnSubscribe { sendNotification(TransferProgress.starting()) }
      .doOnError { sendNotification(TransferProgress.failed()) }
      .doOnComplete { sendNotification(TransferProgress.completed()) }

  private fun createNotification(transferProgress: TransferProgress): Notification =
    notificationManager.createSyncNotification(
      transferProgress.state,
      notificationTitle,
      transferProgress.byteCount,
      transferProgress.bytesTransferred
    )

  /**
   * Specifies that this is a long-running request and should be kept alive by the OS. Also, runs a
   * foreground service under the hood to execute the request showing a notification.
   */
  fun sendNotification(progress: TransferProgress) {
    setForegroundAsync(ForegroundInfo(notificationId, createNotification(progress)))
  }
}
