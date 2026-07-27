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
package org.groundplatform.feature.pdf.render.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import org.groundplatform.feature.pdf.render.PdfConfig
import org.groundplatform.feature.pdf.render.layout.QrPageLayout.Companion.MARGIN
import org.groundplatform.feature.pdf.render.layout.QrPageLayout.Companion.QR_SIZE

class QrPageLayoutTest {

  private val pageWidth = PdfConfig.QR_PAGE_WIDTH
  private val pageHeight = PdfConfig.QR_PAGE_HEIGHT
  private val lineSpacing = PdfConfig.LINE_SPACING

  @Test
  fun `QR frame is a square spanning the page width between the margins`() {
    val layout = QrPageLayout.compute(captionHeight = 10f)

    assertEquals(MARGIN, layout.qrFrame.x)
    assertEquals(QR_SIZE, layout.qrFrame.width)
    assertEquals(QR_SIZE, layout.qrFrame.height)
    assertEquals(pageWidth - MARGIN, layout.qrFrame.right)
  }

  @Test
  fun `caption sits directly below the QR with line spacing between them`() {
    val layout = QrPageLayout.compute(captionHeight = 10f)

    assertEquals(layout.qrFrame.bottom + lineSpacing, layout.captionOffset.y)
  }

  @Test
  fun `caption shares its X with the QR frame`() {
    val layout = QrPageLayout.compute(captionHeight = 10f)

    assertEquals(layout.qrFrame.x, layout.captionOffset.x)
  }

  @Test
  fun `QR and caption are centered vertically on the page`() {
    val captionHeight = 14f
    val layout = QrPageLayout.compute(captionHeight = captionHeight)

    val spaceAbove = layout.qrFrame.y
    val spaceBelow = pageHeight - (layout.captionOffset.y + captionHeight)
    assertEquals(spaceAbove, spaceBelow)
  }

  @Test
  fun `taller captions push the QR further up the page`() {
    val short = QrPageLayout.compute(captionHeight = 10f)
    val tall = QrPageLayout.compute(captionHeight = 30f)

    assertEquals(short.qrFrame.y - 10f, tall.qrFrame.y)
  }
}
