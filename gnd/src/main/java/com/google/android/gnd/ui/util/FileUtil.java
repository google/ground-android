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

package com.google.android.gnd.ui.util;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.RawRes;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import timber.log.Timber;

public class FileUtil {

  private final Context context;

  @Inject
  public FileUtil(@ApplicationContext Context context) {
    this.context = context;
  }

  /**
   * Creates a new file from bitmap and saves under internal app directory
   * /data/data/com.google.android.gnd/files.
   *
   * @throws IOException If path is not accessible or error occurs while saving file
   */
  public File saveBitmap(Bitmap bitmap, String filename) throws IOException {
    File file = new File(context.getFilesDir(), filename);
    try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
    }

    Timber.d("Photo saved : %s", file.getPath());
    return file;
  }

  /**
   * Returns the path of the file saved in the sdcard used for uploading to the provided destination
   * path.
   */
  public File getLocalFileFromRemotePath(String destinationPath) {
    String[] splits = destinationPath.split("/");
    String filename = splits[splits.length - 1];
    File file = new File(context.getFilesDir(), filename);
    if (!file.exists()) {
      Timber.e("File not found: %s", file.getPath());
    }
    return file;
  }

  /**
   * Get a file by name relative to the app's file directory
   * /data/data/com.google.android.gnd/files.
   *
   * <p>If the file doesn't exist, creates a new empty file named {@param filename} in the app's
   * file directory.
   */
  public File getOrCreateFile(String filename) {
    return new File(context.getFilesDir(), filename);
  }

  public File getFileFromRawResource(@RawRes int resourceId, String filename) throws IOException {
    File file = new File(context.getFilesDir(), filename);
    if (!file.exists()) {
      FileUtils.copyInputStreamToFile(context.getResources().openRawResource(resourceId), file);
    }
    return file;
  }

  /** Attempts to delete a file relative to the app's file directory when it exists. */
  public void deleteFile(String filename) {
    File file = new File(context.getFilesDir(), filename);
    if (!file.exists()) {
      return;
    }

    file.delete();
  }
}
