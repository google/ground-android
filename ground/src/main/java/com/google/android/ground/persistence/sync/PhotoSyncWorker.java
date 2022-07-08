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

package com.google.android.ground.persistence.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import com.google.android.ground.R;
import com.google.android.ground.persistence.remote.RemoteStorageManager;
import com.google.android.ground.system.NotificationManager;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import java.io.File;
import java.io.FileNotFoundException;
import timber.log.Timber;

/**
 * A worker that uploads photos from submissions to the FirestoreStorage in the background. The
 * source file and remote destination path are provided in a {@link Data} object. This worker should
 * only run when the device has a network connection.
 */
@HiltWorker
public class PhotoSyncWorker extends BaseWorker {

  private static final String SOURCE_FILE_PATH_PARAM_KEY = "sourceFilePath";
  private static final String DESTINATION_PATH_PARAM_KEY = "destinationPath";

  private final RemoteStorageManager remoteStorageManager;
  private final String localSourcePath;
  private final String remoteDestinationPath;

  @AssistedInject
  public PhotoSyncWorker(
      @Assisted @NonNull Context context,
      @Assisted @NonNull WorkerParameters workerParams,
      RemoteStorageManager remoteStorageManager,
      NotificationManager notificationManager) {
    super(context, workerParams, notificationManager, PhotoSyncWorker.class.hashCode());
    this.remoteStorageManager = remoteStorageManager;
    this.localSourcePath = workerParams.getInputData().getString(SOURCE_FILE_PATH_PARAM_KEY);
    this.remoteDestinationPath = workerParams.getInputData().getString(DESTINATION_PATH_PARAM_KEY);
  }

  static Data createInputData(String sourceFilePath, String destinationPath) {
    return new Data.Builder()
        .putString(SOURCE_FILE_PATH_PARAM_KEY, sourceFilePath)
        .putString(DESTINATION_PATH_PARAM_KEY, destinationPath)
        .build();
  }

  @NonNull
  @Override
  public Result doWork() {
    Timber.d("Attempting photo sync: %s, %s", localSourcePath, remoteDestinationPath);
    File file = new File(localSourcePath);
    if (file.exists()) {
      Timber.d("Starting photo upload: %s, %s", localSourcePath, remoteDestinationPath);
      try {
        remoteStorageManager
            .uploadMediaFromFile(file, remoteDestinationPath)
            .compose(this::notifyTransferState)
            .blockingForEach(this::sendNotification);
        return Result.success();
      } catch (Exception e) {
        FirebaseCrashlytics.getInstance().log("Photo sync failed");
        FirebaseCrashlytics.getInstance().recordException(e);
        Timber.e(e, "Photo sync failed: %s %s", localSourcePath, remoteDestinationPath);
        return Result.retry();
      }
    } else {
      FirebaseCrashlytics.getInstance().log("Photo missing on local device");
      FirebaseCrashlytics.getInstance().recordException(new FileNotFoundException());
      Timber.e("Photo not found %s, %s", localSourcePath, remoteDestinationPath);
      return Result.failure();
    }
  }

  @Override
  public String getNotificationTitle() {
    return getApplicationContext().getString(R.string.uploading_photos);
  }
}
