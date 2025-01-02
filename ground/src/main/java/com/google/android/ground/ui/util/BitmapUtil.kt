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
package com.google.android.ground.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BitmapUtil @Inject internal constructor(@ApplicationContext private val context: Context) {

  /** Retrieves an image for the given url as a [Bitmap]. */
  fun fromUri(url: Uri?): Bitmap {
    require(url != null) { "Uri cannot be null" }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, url))
    } else {
      // Use InputStream and BitmapFactory for older APIs
      context.contentResolver.openInputStream(url)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
      } ?: throw IllegalArgumentException("Unable to open URI: $url")
    }
  }

  fun fromVector(resId: Int): Bitmap {
    val vectorDrawable = ContextCompat.getDrawable(context, resId)!!

    // Specify a bounding rectangle for the Drawable.
    vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
    val bitmap =
      Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888,
      )
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return bitmap
  }
}
