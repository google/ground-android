/*
 * Copyright 2025 Google LLC
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
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.squareup.picasso.Transformation
import java.io.IOException

/**
 * A transformation that rotates a given bitmap based on its EXIF orientation metadata.
 *
 * This class is useful when loading images that may have incorrect orientation due to how they were
 * captured or stored. It reads the EXIF orientation from the image URI and applies the necessary
 * rotation to correct its display.
 *
 * @property uri The URI of the image to be transformed.
 * @property context The context used to access the content resolver for reading EXIF data.
 */
class RotateUsingExif(private val uri: Uri, private val context: Context) : Transformation {
  override fun transform(source: Bitmap): Bitmap {
    val orientation = getOrientationFromExif(uri)
    val rotateDegrees = getRotationDegrees(orientation)
    return rotateBitmap(source, rotateDegrees)
  }

  override fun key(): String = "${uri.hashCode()}"

  private fun rotateBitmap(bitmap: Bitmap, rotateDegrees: Float): Bitmap {
    val matrix = Matrix()
    // Rotate if rotation is non-zero.
    if (rotateDegrees != 0f) {
      matrix.postRotate(rotateDegrees)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true)
  }

  /**
   * Returns the number of degrees a photo should be rotated based on the value of its orientation
   * EXIF tag.
   */
  private fun getRotationDegrees(orientation: Int): Float =
    when (orientation) {
      ExifInterface.ORIENTATION_UNDEFINED,
      ExifInterface.ORIENTATION_NORMAL -> 0f
      ExifInterface.ORIENTATION_ROTATE_90 -> 90f
      ExifInterface.ORIENTATION_ROTATE_180 -> 180f
      ExifInterface.ORIENTATION_ROTATE_270 -> 270f
      else -> throw UnsupportedOperationException("Unsupported photo orientation $orientation")
    }

  /** Returns the EXIF orientation attribute of the JPEG image at the specified URI. */
  private fun getOrientationFromExif(uri: Uri): Int {
    val inputStream =
      context.contentResolver.openInputStream(uri)
        ?: throw IOException("Content resolver returned null for $uri")
    val exif = ExifInterface(inputStream)
    return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
  }
}
