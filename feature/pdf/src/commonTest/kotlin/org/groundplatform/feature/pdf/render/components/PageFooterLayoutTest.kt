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
import kotlin.test.assertTrue
import org.groundplatform.feature.pdf.render.PdfConfig

class PageFooterLayoutTest {

  private val margin = PdfConfig.MARGIN.toFloat()
  private val pageHeight = PdfConfig.PAGE_HEIGHT
  private val usableWidth = PdfConfig.USABLE_WIDTH
  private val pageNumberBand = PdfConfig.PAGE_NUMBER_BAND_WIDTH
  private val footerTextMaxWidth = PdfConfig.FOOTER_TEXT_MAX_WIDTH

  @Test
  fun `footer text anchors against the bottom margin`() {
    val footerHeight = 12f
    val layout = PageFooterLayout.compute(footerHeight = footerHeight)

    assertEquals(margin, layout.footerTextOffset.x)
    assertEquals(pageHeight - margin - footerHeight, layout.footerTextOffset.y)
  }

  @Test
  fun `page number sits in the right-side slot on the same baseline as the footer text`() {
    val layout = PageFooterLayout.compute(footerHeight = 12f)

    assertEquals(margin + usableWidth - pageNumberBand, layout.pageNumberOffset.x)
    assertEquals(layout.footerTextOffset.y, layout.pageNumberOffset.y)
  }

  @Test
  fun `slot widths match their respective configuration constants`() {
    val layout = PageFooterLayout.compute(footerHeight = 12f)

    assertEquals(footerTextMaxWidth, layout.footerMaxWidth)
    assertEquals(pageNumberBand, layout.pageNumberMaxWidth)
  }

  @Test
  fun `footer and page number slots do not overlap`() {
    val layout = PageFooterLayout.compute(footerHeight = 12f)

    val footerRight = layout.footerTextOffset.x + layout.footerMaxWidth
    assertTrue(footerRight <= layout.pageNumberOffset.x)
  }

  @Test
  fun `page number band ends exactly at the right page margin`() {
    val layout = PageFooterLayout.compute(footerHeight = 12f)

    assertEquals(
      margin + usableWidth,
      layout.pageNumberOffset.x + layout.pageNumberMaxWidth,
      "Page number band's right edge must align with the right page margin",
    )
  }

  @Test
  fun `taller footer pushes the baseline higher up the page`() {
    val short = PageFooterLayout.compute(footerHeight = 12f)
    val tall = PageFooterLayout.compute(footerHeight = 30f)

    assertTrue(tall.footerTextOffset.y < short.footerTextOffset.y)
    assertEquals(short.footerTextOffset.y - 18f, tall.footerTextOffset.y)
  }

  @Test
  fun `footer height plus baseline plus bottom margin equals page height`() {
    val footerHeight = 18f
    val layout = PageFooterLayout.compute(footerHeight = footerHeight)

    assertEquals(pageHeight.toFloat(), layout.footerTextOffset.y + footerHeight + margin)
  }

  @Test
  fun `reserve adds the top gap to the footer height`() {
    val footerHeight = 12f

    assertEquals(footerHeight + PdfConfig.FOOTER_TOP_GAP, PageFooterLayout.reserve(footerHeight))
  }
}
