package com.google.android.gnd.system;

import android.app.NotificationChannel;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.gnd.R;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NotificationManager {

  private static final String CHANNEL_1_ID = "channel1";
  private static final String CHANNEL_2_ID = "channel2";

  private Context context;
  private NotificationManagerCompat notificationManager;

  @Inject
  NotificationManager(Context context) {
    this.context = context;
    this.notificationManager = NotificationManagerCompat.from(context);

    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      createNotificationChannels(context);
    }
  }

  @RequiresApi(api = VERSION_CODES.O)
  private void createNotificationChannels(Context context) {
    NotificationChannel channel1 =
        new NotificationChannel(
            CHANNEL_1_ID, "Channel 1", android.app.NotificationManager.IMPORTANCE_HIGH);
    channel1.setDescription("This is Channel 1");

    NotificationChannel channel2 =
        new NotificationChannel(
            CHANNEL_2_ID, "Channel 2", android.app.NotificationManager.IMPORTANCE_LOW);
    channel2.setDescription("This is Channel 2");

    android.app.NotificationManager manager =
        context.getSystemService(android.app.NotificationManager.class);
    manager.createNotificationChannel(channel1);
    manager.createNotificationChannel(channel2);
  }

  public void sendOnChannel() {
    final int progressMax = 100;

    NotificationCompat.Builder notification =
        new NotificationCompat.Builder(context, CHANNEL_2_ID)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle("Downloading")
            .setContentText("Download in progress")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(progressMax, 0, false);

    notificationManager.notify(2, notification.build());

    new Thread(
            () -> {
              SystemClock.sleep(2000);
              for (int i = 0; i < progressMax; i += 10) {
                notification.setProgress(progressMax, i, false);
                notificationManager.notify(2, notification.build());
                SystemClock.sleep(1000);
              }
              notification
                  .setContentText("Download finished")
                  .setProgress(0, 0, false)
                  .setOngoing(false);
              notificationManager.notify(2, notification.build());
            })
        .start();
  }
}
