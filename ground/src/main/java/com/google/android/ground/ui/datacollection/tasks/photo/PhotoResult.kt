/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.datacollection.tasks.photo

import android.graphics.Bitmap
import com.google.android.ground.util.isNotNullOrEmpty

/** Contains the bitmap or path to the photo a user captured or selected. */
data class PhotoResult constructor(val taskId: String, val bitmap: Bitmap?, val path: String?) {

  init {
    check(bitmap != null || path.isNotNullOrEmpty()) {
      "At least one of bitmap or path should be non-null, found " +
        "bitmap=${bitmap != null}, " +
        "path=${path.isNotNullOrEmpty()}"
    }
  }
}
