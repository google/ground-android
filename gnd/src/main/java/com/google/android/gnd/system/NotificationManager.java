package com.google.android.gnd.system;

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
import com.google.android.gnd.persistence.remote.firestore.FirestoreStorageManager.UploadState;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class NotificationManager {

  private static final String CHANNEL_ID = "channel_id";
  private static final int PHOTO_SYNC_ID = 1;

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
    NotificationChannel channel =
        new NotificationChannel(
            CHANNEL_ID, "ground channel", android.app.NotificationManager.IMPORTANCE_LOW);
    android.app.NotificationManager manager =
        context.getSystemService(android.app.NotificationManager.class);
    manager.createNotificationChannel(channel);
  }

  private CharSequence getString(@StringRes int resId) {
    return context.getResources().getString(resId);
  }

  public void createSyncNotification(
      UploadState state, @StringRes int titleResId, int total, int progress) {
    NotificationCompat.Builder notification =
        new Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle(getString(titleResId))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(false)
            .setOngoing(false)
            .setProgress(total, progress, false);

    switch (state) {
      case FAILED:
        notification.setContentText(getString(R.string.failed));
        break;
      case PAUSED:
        notification.setContentText(getString(R.string.paused));
        break;
      case COMPLETED:
        notification.setContentText(getString(R.string.completed));
        break;
      case IN_PROGRESS:
        // only alert once and don't allow cancelling it
        notification
            .setContentText(getString(R.string.in_progress))
            .setOnlyAlertOnce(true)
            .setOngoing(true);
        break;
      default:
        Timber.e("Unknown sync state: %s", state.name());
        break;
    }

    notificationManager.notify(PHOTO_SYNC_ID, notification.build());
  }
}
