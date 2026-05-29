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
package org.groundplatform.ui.components.qrcode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

internal const val QR_SIZE_PX = 512

/**
 * Maximum content size (in UTF-8 bytes) for which a center logo is displayed.
 *
 * Adding a logo in the center of a QR code covers part of the data pattern, so the QR must rely on
 * error correction to remain scannable. A limit of 1,000 bytes is used as a conservative threshold
 * to ensure the QR code remains reliably scannable even with a logo applied.
 */
const val MAX_QR_BYTES_WITH_LOGO = 1000

/**
 * Default relative size of the center logo as a fraction of the QR code's total size.
 *
 * Set to 15% to ensure the logo is clearly visible while remaining within the recovery capacity of
 * high error correction (ECC level H), which can tolerate approximately 30% data loss.
 */
const val LOGO_SIZE_FRACTION = 0.15f
const val PDF_LOGO_SIZE_FRACTION = 0.22f

/**
 * Encodes [content] into a bare QR bitmap, using high error correction when [useHighEcc] is set.
 */
expect fun encodeQrBitmap(content: String, useHighEcc: Boolean): ImageBitmap

/**
 * Generates a QR bitmap for a given [content] with a [logo] in its center. The logo is only applied
 * when one is supplied and the content size is below [MAX_QR_BYTES_WITH_LOGO] to keep the code
 * scannable.
 */
fun generateQrBitmap(
  content: String,
  logo: ImageBitmap?,
  logoSizeFraction: Float = LOGO_SIZE_FRACTION,
): ImageBitmap =
  if (logo == null || content.encodeToByteArray().size > MAX_QR_BYTES_WITH_LOGO) {
    encodeQrBitmap(content, useHighEcc = false)
  } else encodeQrBitmap(content, useHighEcc = true).withCenteredLogo(logo, logoSizeFraction)

/** Draws [logo] centered over the receiver, scaled to [fraction] of its size, into a new bitmap. */
private fun ImageBitmap.withCenteredLogo(logo: ImageBitmap, fraction: Float): ImageBitmap {
  val output = ImageBitmap(width, height)
  val canvas = Canvas(output)
  val paint = Paint().apply { filterQuality = FilterQuality.High }
  canvas.drawImage(this, Offset.Zero, paint)

  val logoWidth = (width * fraction).toInt()
  val logoHeight = (height * fraction).toInt()
  canvas.drawImageRect(
    image = logo,
    dstOffset = IntOffset((width - logoWidth) / 2, (height - logoHeight) / 2),
    dstSize = IntSize(logoWidth, logoHeight),
    paint = paint,
  )
  return output
}
