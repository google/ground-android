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
 * Loads a [Bitmap] from the given [uri] and rotates it if required based on its EXIF orientation
 * metadata.
 *
 * This function ensures that images captured from camera or external sources are displayed in the
 * correct orientation without relying on third-party libraries.
 */
fun loadBitmapWithCorrectOrientation(context: Context, uri: Uri): Bitmap {
  val rotationDegrees = getRotationDegreesFromUri(context, uri)

  context.contentResolver.openInputStream(uri)?.use { inputStream ->
    val bitmap = BitmapFactory.decodeStream(inputStream)
    if (rotationDegrees == 0f) return bitmap

    val matrix = Matrix().apply { postRotate(rotationDegrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  throw IOException("Unable to open input stream for URI: $uri")
}

/**
 * Reads the EXIF orientation header of an image located at the given [uri] and returns the
 * corresponding rotation in degrees.
 *
 * This method uses [ExifInterface] to extract metadata without fully decoding the image, which
 * makes it memory-efficient.
 */
fun getRotationDegreesFromUri(context: Context, uri: Uri): Float {
  context.contentResolver.openInputStream(uri)?.use { inputStream ->
    val exif = ExifInterface(inputStream)
    return when (
      exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    ) {
      ExifInterface.ORIENTATION_ROTATE_90 -> 90f
      ExifInterface.ORIENTATION_ROTATE_180 -> 180f
      ExifInterface.ORIENTATION_ROTATE_270 -> 270f
      else -> 0f
    }
  }
  return 0f
}
