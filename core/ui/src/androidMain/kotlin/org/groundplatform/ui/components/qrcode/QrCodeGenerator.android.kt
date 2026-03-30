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

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
actual fun generateQrBitmap(content: String, useHighEcc: Boolean): ImageBitmap {
  val hints =
    mapOf(
      EncodeHintType.ERROR_CORRECTION to
        if (useHighEcc) ErrorCorrectionLevel.H else ErrorCorrectionLevel.L,
      EncodeHintType.MARGIN to 1,
      EncodeHintType.CHARACTER_SET to "UTF-8",
    )

  val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)

  val width = bitMatrix.width
  val height = bitMatrix.height
  val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
  for (x in 0 until width) {
    for (y in 0 until height) {
      bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
    }
  }

  return bitmap.asImageBitmap()
}
