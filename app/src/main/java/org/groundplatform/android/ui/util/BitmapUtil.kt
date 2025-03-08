/*
 * Copyright 2021 Google LLC
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
package org.groundplatform.android.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BitmapUtil @Inject internal constructor(@ApplicationContext private val context: Context) {

  /**
   * Retrieves an image for the given URI as a [Bitmap].
   *
   * @param url The URI of the image.
   * @return The decoded [Bitmap].
   * @throws IllegalArgumentException If the URI cannot be opened or decoded.
   */
  fun fromUri(url: Uri): Bitmap =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, url))
    } else {
      // Use InputStream and BitmapFactory for older APIs
      context.contentResolver.openInputStream(url)?.use { BitmapFactory.decodeStream(it) }
        ?: throw IllegalArgumentException("Unable to open URI: $url")
    }

  /**
   * Retrieves an image for the given vector resource as a [Bitmap].
   *
   * @param resId The resource ID of the vector drawable.
   * @return The decoded [Bitmap], or null if the resource is not found.
   * @throws IllegalArgumentException If the resource is not a vector drawable.
   */
  fun fromVector(@DrawableRes resId: Int): Bitmap =
    ContextCompat.getDrawable(context, resId)?.toBitmap()
      ?: throw IllegalArgumentException("Resource not found: $resId")
}
