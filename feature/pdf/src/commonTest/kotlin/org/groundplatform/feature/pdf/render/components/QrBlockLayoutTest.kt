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
package org.groundplatform.feature.pdf.render.components

import kotlin.test.Test
import kotlin.test.assertEquals
import org.groundplatform.feature.pdf.render.PdfConfig
import org.groundplatform.feature.pdf.render.components.QrBlockLayout.Companion.QR_SIZE

class QrBlockLayoutTest {

  private val margin = PdfConfig.MARGIN
  private val pageWidth = PdfConfig.PAGE_WIDTH
  private val qrSize = QR_SIZE
  private val lineSpacing = PdfConfig.LINE_SPACING

  private val expectedX = (pageWidth - margin - qrSize)

  @Test
  fun `QR frame is a square anchored at the right margin`() {
    val layout = QrBlockLayout.compute(top = 0f, captionHeight = 10f)

    assertEquals(expectedX, layout.qrFrame.x)
    assertEquals(0f, layout.qrFrame.y)
    assertEquals(qrSize, layout.qrFrame.width)
    assertEquals(qrSize, layout.qrFrame.height)
    assertEquals((pageWidth - margin).toFloat(), layout.qrFrame.right)
  }

  @Test
  fun `caption sits directly below the QR with line spacing between them`() {
    val top = 100f
    val layout = QrBlockLayout.compute(top = top, captionHeight = 10f)

    assertEquals(expectedX, layout.captionOffset.x)
    assertEquals(top + qrSize + lineSpacing, layout.captionOffset.y)
  }

  @Test
  fun `caption shares its X with the QR frame`() {
    val layout = QrBlockLayout.compute(top = 0f, captionHeight = 10f)

    assertEquals(layout.qrFrame.x, layout.captionOffset.x)
  }

  @Test
  fun `nextCursorY accounts for QR, caption, and trailing spacing`() {
    val top = 50f
    val captionHeight = 14f
    val layout = QrBlockLayout.compute(top = top, captionHeight = captionHeight)

    val expectedCaptionTop = top + qrSize + lineSpacing
    assertEquals(expectedCaptionTop + captionHeight + lineSpacing * 2, layout.nextCursorY)
  }
}
