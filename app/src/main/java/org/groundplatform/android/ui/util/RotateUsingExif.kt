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
import androidx.core.graphics.scale
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
class RotateUsingExif(
  private val uri: Uri,
  private val context: Context,
  private val maxWidth: Int = 2048,
  private val maxHeight: Int = 2048,
) : Transformation {
  override fun transform(source: Bitmap): Bitmap {
    val orientation = getOrientationFromExif(uri)
    val rotateDegrees = getRotationDegrees(orientation)

    val resizedBitmap = resizeBitmapIfNeeded(source, maxWidth, maxHeight)

    return if (rotateDegrees != 0f) {
      rotateBitmap(resizedBitmap, rotateDegrees)
    } else {
      resizedBitmap
    }
  }

  override fun key(): String = "${uri.hashCode()}_${maxWidth}x${maxHeight}"

  private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    if (width <= maxWidth && height <= maxHeight) {
      return bitmap
    }

    val scaleFactor = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)

    val newWidth = (width * scaleFactor).toInt()
    val newHeight = (height * scaleFactor).toInt()

    val resized = bitmap.scale(newWidth, newHeight)

    if (resized != bitmap) {
      bitmap.recycle()
    }

    return resized
  }

  private fun rotateBitmap(bitmap: Bitmap, rotateDegrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(rotateDegrees)

    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    if (rotated != bitmap) {
      bitmap.recycle()
    }

    return rotated
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
