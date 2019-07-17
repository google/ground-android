/*
 * Copyright 2019 Google LLC
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

package com.google.android.gnd.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A worker that downloads files to the device in the background. The target URL and file name are
 * provided in a {@link Data} object. This worker should only run when the device has a network
 * connection.
 */
public class FileDownloadWorker extends Worker {
  private static final String TARGET_URL = "url";
  private static final String FILENAME = "filename";
  private static final int BUFFER_SIZE = 4096;
  private final Context context;

  public FileDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
    this.context = context;
  }

  private static final String TAG = FileDownloadWorker.class.getSimpleName();

  /** Creates input data for the FileDownloadWorker. */
  public static Data createInputData(String url, String filename) {
    return new Data.Builder().putString(TARGET_URL, url).putString(FILENAME, filename).build();
  }

  /**
   * Downloads a file from a given url and saves it to the application's file directory. The
   * directory is given by the provided context. Returns a successful result containing the filename
   * of the written file upon success.
   */
  @NonNull
  @Override
  public Result doWork() {
    String url = getInputData().getString(TARGET_URL);
    // TODO: If the filename is no good, fail.
    String filename = getInputData().getString(FILENAME);

    try {
      InputStream is = new URL(url).openStream();
      FileOutputStream fos = context.openFileOutput(filename, context.MODE_PRIVATE);
      byte[] byteChunk = new byte[BUFFER_SIZE];
      int n;
      while ((n = is.read(byteChunk)) > 0) {
        fos.write(byteChunk, 0, n);
      }
      is.close();
      fos.close();
      return Result.success(new Data.Builder().putString(FILENAME, filename).build());
    } catch (IOException e) {
      Log.d(TAG, e.getMessage());
    }
    return Result.failure();
  }
}
