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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.groundplatform.feature.pdf.render.fitInside
import org.groundplatform.feature.pdf.render.image.PdfImage
import org.groundplatform.feature.pdf.render.image.PdfImageSet
import org.groundplatform.feature.pdf.render.layout.QrBlockLayout
import org.groundplatform.feature.pdf.render.layout.TableLayout
import org.groundplatform.feature.pdf.render.pointsToRenderPixels
import org.groundplatform.ui.components.qrcode.PDF_LOGO_SIZE_FRACTION
import org.groundplatform.ui.components.qrcode.generateQrBitmap
import timber.log.Timber

/**
 * Android implementation of [PdfImageProvider].
 *
 * Bitmaps are decoded and scaled to their final on-page pixel size here so the renderer can draw
 * them as-is without any further bitmap work.
 *
 * @param context application context used for resource access and file lookups.
 * @param logoDrawableRes resource id of the centre logo bitmap. Caller supplies the app's branding.
 */
// TODO: Add equivalent iOS implementations for PDF feature
// Issue URL: https://github.com/google/ground-android/issues/3775
class AndroidPdfImageProvider(
  private val context: Context,
  @DrawableRes private val logoDrawableRes: Int,
) : PdfImageProvider {

  private val qrMaxPx = pointsToRenderPixels(QrBlockLayout.QR_SIZE)
  private val photoMaxWidthPx = pointsToRenderPixels(TableLayout.ANSWER_TEXT_WIDTH.toFloat())
  private val photoMaxHeightPx = pointsToRenderPixels(TableLayout.PHOTO_MAX_HEIGHT.toFloat())

  override suspend fun load(qrContent: String?, photoFilenames: Set<String>): PdfImageSet =
    coroutineScope {
      val deferredQr = qrContent?.let { content ->
        async {
          generateQrCodeBitmap(content)?.let { bitmap ->
            PdfImageSet.ImageRef.Qr to bitmap
          }
        }
      }

      val deferredPhotos =
        photoFilenames
          .filter { it.isNotEmpty() }
          .map { filename ->
            async {
              loadPhotoBitmap(filename)?.let { bitmap ->
                PdfImageSet.ImageRef.Photo(filename) to bitmap
              }
            }
          }

      val results = (listOfNotNull(deferredQr) + deferredPhotos).awaitAll().filterNotNull()

      val images = mutableMapOf<PdfImageSet.ImageRef, PdfImage>()
      val bitmapsToRelease = mutableListOf<Bitmap>()
      results.forEach { (ref, bitmap) ->
        bitmapsToRelease += bitmap
        images[ref] = PdfImage(bitmap)
      }

      PdfImageSet(images = images, onRelease = { bitmapsToRelease.forEach(Bitmap::recycle) })
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
          .scaledToFit(qrMaxPx, qrMaxPx)
      }
      .onFailure { Timber.e(it, "Failed to generate QR code bitmap for PDF report") }
      .getOrNull()

  private fun loadPhotoBitmap(remoteFilename: String): Bitmap? {
    val rootDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
    val file = File(rootDir, remoteFilename.substringAfterLast('/'))
    if (!file.exists()) return null
    return runCatching { decodeScaledAndOriented(file) }
      .onFailure { Timber.e(it, "Failed to decode photo for PDF report") }
      .getOrNull()
  }

  /**
   * Decodes the photo subsampled to roughly the largest size it can occupy in the PDF, scales it to
   * fit the photo box, then applies the EXIF orientation.
   */
  private fun decodeScaledAndOriented(file: File): Bitmap? {
    val path = file.absolutePath
    val degrees = runCatching { ExifInterface(path).rotationDegrees }.getOrDefault(0)
    // A 90°/270° EXIF rotation swaps width and height, so size the decode box accordingly.
    val swapAxes = degrees == 90 || degrees == 270
    val boxWidth = if (swapAxes) photoMaxHeightPx else photoMaxWidthPx
    val boxHeight = if (swapAxes) photoMaxWidthPx else photoMaxHeightPx

    val decoded = decodeSubsampled(path, boxWidth, boxHeight) ?: return null
    return decoded.scaledToFit(boxWidth, boxHeight).rotated(degrees)
  }

  /** Decodes the photo subsampled to roughly the [maxWidth] × [maxHeight] box it will occupy. */
  private fun decodeSubsampled(path: String, maxWidth: Int, maxHeight: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val options =
      BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxWidth, maxHeight)
      }
    return BitmapFactory.decodeFile(path, options)
  }
}

/**
 * Largest power-of-two sample size that keeps the decoded bitmap at or above the [maxWidth] ×
 * [maxHeight].
 */
internal fun calculateInSampleSize(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
  var sampleSize = 1
  while (width / (sampleSize * 2) >= maxWidth || height / (sampleSize * 2) >= maxHeight) {
    sampleSize *= 2
  }
  return sampleSize
}

/**
 * Returns the receiver scaled down to fit [maxWidth] × [maxHeight], preserving aspect ratio and
 * never upscaling.
 */
internal fun Bitmap.scaledToFit(maxWidth: Int, maxHeight: Int): Bitmap {
  val fitted = fitInside(width, height, maxWidth, maxHeight)
  val targetWidth = fitted.width.roundToInt().coerceAtLeast(1)
  val targetHeight = fitted.height.roundToInt().coerceAtLeast(1)
  if (targetWidth == width && targetHeight == height) return this
  return scale(targetWidth, targetHeight).also { if (it !== this) recycle() }
}

/** Returns the receiver rotated [degrees] clockwise, or unchanged when no rotation is needed. */
private fun Bitmap.rotated(degrees: Int): Bitmap {
  if (degrees == 0) return this
  val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
  return runCatching { Bitmap.createBitmap(this, 0, 0, width, height, matrix, true) }
    .getOrElse {
      Timber.w(it, "Failed to rotate photo by $degrees°, returning unrotated bitmap")
      null
    }
    ?.also { if (it !== this) recycle() } ?: this
}
