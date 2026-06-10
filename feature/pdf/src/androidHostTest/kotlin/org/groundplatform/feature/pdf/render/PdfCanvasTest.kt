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
package org.groundplatform.feature.pdf.render

import android.graphics.Bitmap
import android.graphics.RectF
import android.text.StaticLayout
import android.text.TextPaint
import org.groundplatform.feature.pdf.render.image.PdfImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfCanvasTest {
  @Test
  fun `MeasurementPdfCanvas ignores every call`() {
    val layout = StaticLayout.Builder.obtain("body", 0, "body".length, TextPaint(), 100).build()
    val image = PdfImage(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    with(MeasurementPdfCanvas) {
      startPage(pageNumber = 1)
      drawStaticLayout(layout, x = 0f, y = 0f)
      drawImage(image, RectF(0f, 0f, 10f, 10f), smoothScaling = true)
      drawLine(0f, 0f, 10f, 10f)
      finishPage()
    }
  }
}
