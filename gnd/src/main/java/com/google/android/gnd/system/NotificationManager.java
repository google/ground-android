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

package com.google.android.gnd.system;

import android.app.Notification;
import android.app.NotificationChannel;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.gnd.R;
import com.google.android.gnd.persistence.remote.TransferProgress.UploadState;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class NotificationManager {

  public static final int SYNC_NOTIFICATION_ID = 1;
  public static final int ALWAYS_ON_NOTIFICATION_ID = 2;

  private static final String CHANNEL_ID = "channel_id";
  private static final String CHANNEL_NAME = "sync channel";

  private Context context;
  private NotificationManagerCompat manager;

  @Inject
  NotificationManager(Context context) {
    this.context = context;
    this.manager = NotificationManagerCompat.from(context);

    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      createNotificationChannels(context);
    }
  }

  @RequiresApi(api = VERSION_CODES.O)
  private void createNotificationChannels(Context context) {
    NotificationChannel channel =
        new NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, android.app.NotificationManager.IMPORTANCE_LOW);
    android.app.NotificationManager manager =
        context.getSystemService(android.app.NotificationManager.class);
    manager.createNotificationChannel(channel);
  }

  public Notification createForegroundServiceNotification() {
    NotificationCompat.Builder notification =
        new Builder(context, CHANNEL_ID)
            // TODO: Use a better icon
            .setSmallIcon(R.drawable.ground_logo)
            .setContentText(context.getString(R.string.app_running))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true);

    return notification.build();
  }

  public void createSyncNotification(
      UploadState state, @StringRes int titleResId, int total, int progress) {
    NotificationCompat.Builder notification =
        new Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle(context.getString(titleResId))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(false)
            .setOngoing(false)
            .setProgress(total, progress, false);

    switch (state) {
      case STARTING:
        notification.setContentText(context.getString(R.string.starting));
        break;
      case IN_PROGRESS:
        notification
            .setContentText(context.getString(R.string.in_progress))
            // only alert once and don't allow cancelling it
            .setOnlyAlertOnce(true)
            .setOngoing(true);
        break;
      case PAUSED:
        notification.setContentText(context.getString(R.string.paused));
        break;
      case FAILED:
        notification.setContentText(context.getString(R.string.failed));
        break;
      case COMPLETED:
        notification.setContentText(context.getString(R.string.completed));
        break;
      default:
        Timber.e("Unknown sync state: %s", state.name());
        break;
    }

    manager.notify(SYNC_NOTIFICATION_ID, notification.build());

    // remove if completed
    if (state == UploadState.COMPLETED) {
      manager.cancel(SYNC_NOTIFICATION_ID);
    }
  }
}
