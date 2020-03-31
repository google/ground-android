package com.google.android.gnd.system.service;

import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import com.google.android.gnd.inject.GndWorkerFactory;
import com.google.android.gnd.system.NotificationManager;
import dagger.android.DaggerService;
import javax.inject.Inject;
import timber.log.Timber;

public class ForegroundService extends DaggerService {

  @Inject GndWorkerFactory workerFactory;
  @Inject NotificationManager notificationManager;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Timber.d("onStartCommand: ");
    startForeground(
        NotificationManager.ALWAYS_ON_NOTIFICATION_ID,
        notificationManager.createForegroundServiceNotification());

    // Set custom worker factory that allow Workers to use Dagger injection.
    // TODO(github.com/google/dagger/issues/1183): Remove once Workers support injection.
    WorkManager.initialize(
        this, new Configuration.Builder().setWorkerFactory(workerFactory).build());

    // stopSelf();
    return START_NOT_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
