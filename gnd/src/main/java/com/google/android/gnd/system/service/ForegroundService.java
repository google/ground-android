package com.google.android.gnd.system.service;

import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.google.android.gnd.system.NotificationManager;
import dagger.android.DaggerService;
import javax.inject.Inject;
import timber.log.Timber;

public class ForegroundService extends DaggerService {

  @Inject NotificationManager notificationManager;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Timber.d("onStartCommand: ");
    startForeground(
        NotificationManager.ALWAYS_ON_NOTIFICATION_ID,
        notificationManager.createForegroundServiceNotification());

    // do heavy work on a background thread
    // stopSelf();
    return START_NOT_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
