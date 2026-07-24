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
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.test.assertFailsWith
import org.groundplatform.feature.pdf.render.image.PdfImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DocumentPdfCanvasTest {

  private val canvas = DocumentPdfCanvas(PdfDocument())

  @Test
  fun `drawLine before a page is started fails`() {
    assertFailsWith<IllegalStateException> { canvas.drawLine(0f, 0f, 10f, 10f) }
  }

  @Test
  fun `drawImage before a page is started fails`() {
    val image = PdfImage(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    assertFailsWith<IllegalStateException> {
      canvas.drawImage(image, RectF(0f, 0f, 10f, 10f), smoothScaling = false)
    }
  }

  @Test
  fun `drawStaticLayout before a page is started fails`() {
    val layout = StaticLayout.Builder.obtain("body", 0, 4, TextPaint(), 100).build()
    assertFailsWith<IllegalStateException> { canvas.drawStaticLayout(layout, x = 0f, y = 0f) }
  }

  @Test
  fun `finishPage with no page open does nothing`() {
    canvas.finishPage()
  }

  @Test
  fun `drawMapOverlay before a page is started fails`() {
    assertFailsWith<IllegalStateException> {
      canvas.drawMapOverlay(COMPASS_OVERLAY, darkBasemap = false)
    }
    assertFailsWith<IllegalStateException> {
      canvas.drawMapOverlay(LINE_OVERLAY, darkBasemap = false)
    }
    assertFailsWith<IllegalStateException> {
      canvas.drawMapOverlay(TEXT_OVERLAY, darkBasemap = false)
    }
  }

  private companion object {
    val COMPASS_OVERLAY =
      MapOverlay.Polygon(listOf(PdfOffset(0f, 0f), PdfOffset(10f, 0f), PdfOffset(5f, 10f)))
    val LINE_OVERLAY = MapOverlay.Line(PdfLine(0f, 0f, 10f, 0f))
    val TEXT_OVERLAY = MapOverlay.Text("N", boxWidth = 50, offset = PdfOffset(0f, 0f))
  }
}
