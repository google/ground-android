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
package com.google.android.ground.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.ground.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "channel_id"
private const val CHANNEL_NAME = "sync channel"

@Singleton
class NotificationManager
@Inject
internal constructor(@param:ApplicationContext private val context: Context) {

  init {
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      createNotificationChannels(context)
    }
  }

  @RequiresApi(api = VERSION_CODES.O)
  private fun createNotificationChannels(context: Context) {
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }

  enum class UploadState {
    STARTING,
    IN_PROGRESS,
    PAUSED,
    FAILED,
    COMPLETED
  }

  fun createSyncNotification(
    state: UploadState,
    title: String,
    total: Int,
    progress: Int
  ): Notification {
    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_sync)
        .setContentTitle(title)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setOnlyAlertOnce(false)
        .setOngoing(false)
        .setProgress(total, progress, false)
    when (state) {
      UploadState.STARTING -> notification.setContentText(context.getString(R.string.starting))
      UploadState.IN_PROGRESS ->
        notification
          .setContentText(
            context.getString(R.string.in_progress)
          ) // only alert once and don't allow cancelling it
          .setOnlyAlertOnce(true)
          .setOngoing(true)
      UploadState.PAUSED -> notification.setContentText(context.getString(R.string.paused))
      UploadState.FAILED -> notification.setContentText(context.getString(R.string.failed))
      UploadState.COMPLETED -> notification.setContentText(context.getString(R.string.completed))
    }
    return notification.build()
  }
}
