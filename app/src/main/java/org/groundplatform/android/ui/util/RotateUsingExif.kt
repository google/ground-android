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
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

/**
 * Loads a [Bitmap] from the given [uri] and applies the correct rotation based on its EXIF
 * orientation.
 *
 * This method ensures that images—especially those captured from the camera or selected from
 * external sources— are displayed in the proper orientation by reading the EXIF metadata and
 * rotating the bitmap accordingly.
 */
fun loadBitmapWithCorrectOrientation(context: Context, uri: Uri): Bitmap {
  val orientation = getOrientationFromExif(context, uri)
  val rotationDegrees = getRotationDegrees(orientation)

  context.contentResolver.openInputStream(uri)?.use { inputStream ->
    val bitmap = BitmapFactory.decodeStream(inputStream)
    if (rotationDegrees == 0f) return bitmap

    val matrix = Matrix().apply { postRotate(rotationDegrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  throw IOException("Unable to open input stream for URI: $uri")
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

/** Reads the EXIF orientation tag from the image at the given [uri]. */
private fun getOrientationFromExif(context: Context, uri: Uri): Int {
  val inputStream =
    context.contentResolver.openInputStream(uri)
      ?: throw IOException("Content resolver returned null for $uri")
  val exif = ExifInterface(inputStream)
  return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
}
