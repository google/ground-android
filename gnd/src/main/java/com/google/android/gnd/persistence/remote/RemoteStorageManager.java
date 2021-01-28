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

package com.google.android.gnd.persistence.remote;

import android.net.Uri;
import com.google.android.gnd.rx.annotations.Cold;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.io.File;

/**
 * Defines API for accessing files in remote storage. Implementations must ensure all subscriptions
 * are run in a background thread (i.e., not the Android main thread).
 */
public interface RemoteStorageManager {

  /** Returns a URL that can be used to download a file at the specified path in remote storage. */
  @Cold
  Single<Uri> getDownloadUrl(String remoteDestinationPath);

  /** Uploads file to a remote path, streaming progress in the returned {@link Flowable}. */
  @Cold
  Flowable<TransferProgress> uploadMediaFromFile(File file, String remoteDestinationPath);
}
