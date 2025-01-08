/*
 * Copyright 2023 Google LLC
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

package com.google.android.ground.ui.map.gms.mog

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import java.io.ByteArrayOutputStream

/** Utility for modifying images. */
object ImageEditor {
  /**
   * Decodes the provided source image, making all pixels for which the provided lambda returns
   * `true` fully transparent (alpha = 0). Returns the modified images as a byte array.
   */
  fun setTransparentIf(
    sourceImage: ByteArray,
    isTransparent: (bitmap: Bitmap, x: Int, y: Int) -> Boolean,
  ): ByteArray {
    val opts = BitmapFactory.Options()
    opts.inMutable = true
    val bitmap = BitmapFactory.decodeByteArray(sourceImage, 0, sourceImage.size, opts)
    bitmap.setHasAlpha(true)
    for (y in 0 until bitmap.height) {
      for (x in 0 until bitmap.width) {
        if (isTransparent(bitmap, x, y)) {
          bitmap.setPixel(x, y, Color.TRANSPARENT)
        }
      }
    }
    // Android doesn't implement encoders for uncompressed format, so we must compress the returned
    // tile so that it can be later encoded by Maps SDK. Experimentally, decompressing JPG and
    // compressing WEBP each tile adds on the order of 1ms to each tile which we can consider
    // negligible.
    val stream = ByteArrayOutputStream()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, stream)
    } else {
      bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream)
    }
    return stream.toByteArray()
  }
}
