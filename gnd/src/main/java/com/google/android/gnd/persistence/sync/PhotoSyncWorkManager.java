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

package com.google.android.gnd.persistence.sync;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.ui.util.FileUtil;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Provider;
import timber.log.Timber;

/** Enqueues photo upload work to be done in the background. */
public class PhotoSyncWorkManager extends BaseWorkManager {

  private final LocalValueStore localValueStore;
  private final FileUtil fileUtil;

  @Inject
  public PhotoSyncWorkManager(
      Provider<WorkManager> workManagerProvider,
      LocalValueStore localValueStore,
      FileUtil fileUtil) {
    super(workManagerProvider);
    this.localValueStore = localValueStore;
    this.fileUtil = fileUtil;
  }

  @NonNull
  @Override
  Class<PhotoSyncWorker> getWorkerClass() {
    return PhotoSyncWorker.class;
  }

  @NonNull
  @Override
  protected NetworkType preferredNetworkType() {
    return localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly()
        ? NetworkType.UNMETERED
        : NetworkType.CONNECTED;
  }

  /**
   * Enqueues a worker that uploads selected/captured photo to the remote FirestoreStorage once a
   * network connection is available. The returned {@code Completable} completes immediately as soon
   * as the worker is added to the work queue (not once the sync job completes).
   */
  public void enqueueSyncWorker(@NonNull String remotePath) {
    File localFile = fileUtil.getLocalFileFromRemotePath(remotePath);

    if (!localFile.exists()) {
      Timber.e("Local file not found: %s", localFile.getPath());
      return;
    }

    Data inputData = PhotoSyncWorker.createInputData(localFile.getPath(), remotePath);
    OneTimeWorkRequest request = buildWorkerRequest(inputData);
    getWorkManager().enqueue(request);
  }
}
