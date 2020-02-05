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
import android.graphics.BitmapFactory;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.inject.Inject;

public class FileUtil {

  private static final String TAG = FileUtil.class.getName();
  private final Context context;

  @Inject
  public FileUtil(Context context) {
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

    Log.d(TAG, "Photo saved : " + file.getPath());
    return file;
  }

  public File getFileFromFilename(String filename) throws FileNotFoundException {
    File file = new File(context.getFilesDir(), filename);
    if (!file.exists()) {
      throw new FileNotFoundException("File not found: " + filename);
    }
    return file;
  }

  /** Load bitmap from a file path. */
  @Nullable
  public static Bitmap createBitmapFromPath(String path) {
    File file = new File(path);
    if (!file.exists()) {
      Log.e(TAG, "File not found: " + path);
      return null;
    }
    return BitmapFactory.decodeFile(path);
  }
}
