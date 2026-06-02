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
package org.groundplatform.feature.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.roundToInt
import org.groundplatform.feature.pdf.render.PdfConfig
import org.groundplatform.feature.pdf.render.image.PdfImage
import org.groundplatform.feature.pdf.render.image.PdfImageSet
import org.groundplatform.feature.pdf.render.pointsToRenderPixels
import org.groundplatform.ui.components.qrcode.PDF_LOGO_SIZE_FRACTION
import org.groundplatform.ui.components.qrcode.generateQrBitmap

/**
 * Android implementation of [PdfImageProvider].
 *
 * Bitmaps are decoded and scaled to their final on-page pixel size here so the renderer can draw
 * them as-is without any further bitmap work.
 *
 * @param context application context used for resource access and file lookups.
 * @param logoDrawableRes resource id of the centre logo bitmap. Caller supplies the app's branding.
 */
class AndroidPdfImageProvider(
  private val context: Context,
  @DrawableRes private val logoDrawableRes: Int,
) : PdfImageProvider {

  private val qrMaxPx = pointsToRenderPixels(PdfConfig.QR_SIZE.toFloat())
  private val photoMaxWidthPx = pointsToRenderPixels(PdfConfig.TABLE_ANSWER_TEXT_WIDTH.toFloat())
  private val photoMaxHeightPx = pointsToRenderPixels(PdfConfig.PHOTO_MAX_HEIGHT.toFloat())

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
          .downscaledTo(qrMaxPx, qrMaxPx)
      }
      .getOrNull()

  private fun loadPhotoBitmap(remoteFilename: String): Bitmap? {
    val rootDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
    val filename = remoteFilename.substringAfterLast('/')
    val file = File(rootDir, filename)
    if (!file.exists()) return null
    val decoded = runCatching { decodeSubsampled(file.absolutePath) }.getOrNull() ?: return null
    val oriented = applyExifOrientation(file, decoded)
    return oriented.downscaledTo(photoMaxWidthPx, photoMaxHeightPx)
  }

  /** Decodes the photo subsampled to roughly the largest size it can occupy in the PDF. */
  private fun decodeSubsampled(path: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    // Orientation isn't known yet, so size against the larger target on both axes to be safe.
    val target = maxOf(photoMaxWidthPx, photoMaxHeightPx)
    val options =
      BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, target)
      }
    return BitmapFactory.decodeFile(path, options)
  }

  /** Largest power-of-two sample size that keeps both dimensions at or above [target] pixels. */
  private fun calculateInSampleSize(width: Int, height: Int, target: Int): Int {
    var sampleSize = 1
    while (width / (sampleSize * 2) >= target && height / (sampleSize * 2) >= target) {
      sampleSize *= 2
    }
    return sampleSize
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

/**
 * Returns a bitmap scaled down to fit [maxWidthPx] × [maxHeightPx], preserving aspect ratio and
 * never upscaling. Uses progressive halving for efficiency; intermediate bitmaps are recycled.
 */
private fun Bitmap.downscaledTo(maxWidthPx: Int, maxHeightPx: Int): Bitmap {
  if (width <= maxWidthPx && height <= maxHeightPx) return this
  val ratio = minOf(maxWidthPx.toFloat() / width, maxHeightPx.toFloat() / height)
  val targetWidth = (width * ratio).roundToInt().coerceAtLeast(1)
  val targetHeight = (height * ratio).roundToInt().coerceAtLeast(1)

  var current = this
  var w = width
  var h = height
  while (w / 2 >= targetWidth && h / 2 >= targetHeight) {
    w /= 2
    h /= 2
    val halved = current.scale(w, h)
    if (current !== this) current.recycle()
    current = halved
  }
  val result = current.scale(targetWidth, targetHeight)
  if (current !== this) current.recycle()
  return result
}
