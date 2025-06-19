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
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

class BitmapUtil @Inject internal constructor(@ApplicationContext private val context: Context) {

  fun fromUri(uri: Uri): Bitmap {
    val options = BitmapFactory.Options().apply {
      inJustDecodeBounds = true
    }
    context.contentResolver.openInputStream(uri)?.use {
      BitmapFactory.decodeStream(it, null, options)
    }

    // Compute a sample size to avoid OOM
    options.inSampleSize = calculateInSampleSize(options, reqWidth = 1024, reqHeight = 1024)
    options.inJustDecodeBounds = false

    return context.contentResolver.openInputStream(uri)?.use {
      BitmapFactory.decodeStream(it, null, options)
    } ?: throw IOException("Unable to open URI: $uri")
  }

  private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
  ): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
      val halfHeight: Int = height / 2
      val halfWidth: Int = width / 2

      while ((halfHeight / inSampleSize) >= reqHeight &&
        (halfWidth / inSampleSize) >= reqWidth) {
        inSampleSize *= 2
      }
    }

    return inSampleSize
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
