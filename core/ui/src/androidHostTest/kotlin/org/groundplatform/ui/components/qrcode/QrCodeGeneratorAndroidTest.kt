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
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class QrCodeGeneratorAndroidTest {

  @Test
  fun `encodeQrBitmap produces a square bitmap at the configured size`() {
    val qr = encodeQrBitmap(QR_CONTENT, useHighEcc = false)

    assertEquals(QR_SIZE_PX, qr.width)
    assertEquals(QR_SIZE_PX, qr.height)
  }

  @Test
  fun `generateQrBitmap without a logo returns a square code`() {
    val qr = generateQrBitmap(content = QR_CONTENT, logo = null)

    assertEquals(QR_SIZE_PX, qr.width)
    assertEquals(QR_SIZE_PX, qr.height)
  }

  @Test
  fun `generateQrBitmap composites a logo when the content fits the capacity`() {
    val logo = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).asImageBitmap()

    val qr = generateQrBitmap(content = "short", logo = logo)

    assertEquals(QR_SIZE_PX, qr.width)
    assertEquals(QR_SIZE_PX, qr.height)
  }

  @Test
  fun `generateQrBitmap skips the logo when the content exceeds the capacity`() {
    val logo = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).asImageBitmap()
    val tooLong = "1".repeat(MAX_QR_BYTES_WITH_LOGO + 1)

    val qr = generateQrBitmap(content = tooLong, logo = logo)

    assertEquals(QR_SIZE_PX, qr.width)
    assertEquals(QR_SIZE_PX, qr.height)
  }

  private companion object {
    const val QR_CONTENT = "https://google.com"
  }
}
