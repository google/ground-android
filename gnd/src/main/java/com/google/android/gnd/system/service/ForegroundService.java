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
  public void onCreate() {
    super.onCreate();
    // Set custom worker factory that allow Workers to use Dagger injection.
    // TODO(github.com/google/dagger/issues/1183): Remove once Workers support injection.
    WorkManager.initialize(
        this, new Configuration.Builder().setWorkerFactory(workerFactory).build());
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Timber.d("onStartCommand: ");
    startForeground(
        NotificationManager.ALWAYS_ON_NOTIFICATION_ID,
        notificationManager.createForegroundServiceNotification());

    // stopSelf();
    return START_NOT_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
