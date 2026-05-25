/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.ui.system.pdf.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.File
import org.groundplatform.ui.components.qrcode.PDF_LOGO_SIZE_FRACTION
import org.groundplatform.ui.components.qrcode.generateQrBitmap

/**
 * Android implementation of [PdfImageProvider].
 *
 * @param context application context used for resource access and file lookups.
 * @param logoDrawableRes resource id of the centre logo bitmap. Caller supplies the app's branding.
 */
class AndroidPdfImageProvider(
  private val context: Context,
  @DrawableRes private val logoDrawableRes: Int,
) : PdfImageProvider {

  override suspend fun load(qrContent: String?, photoFilenames: Set<String>): PdfImageSet {
    val images = mutableMapOf<PdfImageSet.ImageRef, PdfImage>()
    val bitmapsToRelease = mutableListOf<Bitmap>()

    qrContent?.let { content ->
      generateQrCodeBitmap(content)?.let { bitmap ->
        bitmapsToRelease += bitmap
        images[PdfImageSet.ImageRef.Qr] = PdfImage(bitmap)
      }
    }

    photoFilenames
      .filter { it.isNotEmpty() }
      .forEach { filename ->
        loadPhotoBitmap(filename)?.let { bitmap ->
          bitmapsToRelease += bitmap
          images[PdfImageSet.ImageRef.Photo(filename)] = PdfImage(bitmap)
        }
      }

    return PdfImageSet(images) { bitmapsToRelease.forEach(Bitmap::recycle) }
  }

  private fun generateQrCodeBitmap(content: String): Bitmap? =
    runCatching {
        generateQrBitmap(
            content = content,
            logo =
              BitmapFactory.decodeResource(context.resources, logoDrawableRes)?.asImageBitmap(),
            logoSizeFraction = PDF_LOGO_SIZE_FRACTION,
          )
          .asAndroidBitmap()
      }
      .getOrNull()

  private fun loadPhotoBitmap(remoteFilename: String): Bitmap? {
    val rootDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
    val filename = remoteFilename.substringAfterLast('/')
    val file = File(rootDir, filename)
    if (!file.exists()) return null
    val bitmap =
      runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull() ?: return null
    return applyExifOrientation(file, bitmap)
  }

  /**
   * Rotates [bitmap] to match the EXIF orientation in [file]. Returns the original bitmap if no
   * rotation is needed.
   */
  private fun applyExifOrientation(file: File, bitmap: Bitmap): Bitmap {
    val degrees = runCatching { ExifInterface(file.absolutePath).rotationDegrees }.getOrDefault(0)
    if (degrees == 0) return bitmap

    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return runCatching {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
      }
      .getOrNull()
      ?.also { if (it != bitmap) bitmap.recycle() } ?: bitmap
  }
}
