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
package com.google.android.ground.ui.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class FileUtil @Inject constructor(@param:ApplicationContext private val context: Context) {
  /**
   * Get a file by name relative to the app's file directory
   * /data/data/com.google.android.ground/files.
   *
   * If the file doesn't exist, creates a new empty file named {@param filename} in the app's file
   * directory.
   */
  fun getOrCreateFile(filename: String): File = File(context.filesDir, filename)

  /** Attempts to delete a file relative to the app's file directory when it exists. */
  fun deleteFile(filename: String) {
    val file = getOrCreateFile(filename)
    if (!file.exists()) {
      return
    }
    file.delete()
  }
}
