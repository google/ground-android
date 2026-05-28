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
package org.groundplatform.ui.system.pdf.components

import org.groundplatform.ui.system.pdf.PdfConfig.FOOTER_TEXT_MAX_WIDTH
import org.groundplatform.ui.system.pdf.PdfConfig.MARGIN
import org.groundplatform.ui.system.pdf.PdfConfig.PAGE_HEIGHT
import org.groundplatform.ui.system.pdf.PdfConfig.PAGE_NUMBER_BAND_WIDTH
import org.groundplatform.ui.system.pdf.PdfConfig.USABLE_WIDTH
import org.groundplatform.ui.system.pdf.PdfRenderer

/**
 * Pre-computed layout for the page footer with separate left and right slots.
 *
 * @param footerTextOffset The top-left position where the footer text begins.
 * @param footerMaxWidth The maximum width available for the footer text.
 * @param pageNumberOffset The top-left position where the page number begins.
 * @param pageNumberMaxWidth The maximum width available for the page number
 */
internal data class PageFooterLayout(
  val footerTextOffset: PdfRenderer.Offset,
  val footerMaxWidth: Int,
  val pageNumberOffset: PdfRenderer.Offset,
  val pageNumberMaxWidth: Int,
) {
  companion object {
    fun compute(footerHeight: Float): PageFooterLayout {
      val top = PAGE_HEIGHT - MARGIN - footerHeight
      val left = MARGIN.toFloat()
      val pageNumberLeft = left + USABLE_WIDTH - PAGE_NUMBER_BAND_WIDTH
      return PageFooterLayout(
        footerTextOffset = PdfRenderer.Offset(left, top),
        footerMaxWidth = FOOTER_TEXT_MAX_WIDTH,
        pageNumberOffset = PdfRenderer.Offset(pageNumberLeft, top),
        pageNumberMaxWidth = PAGE_NUMBER_BAND_WIDTH,
      )
    }
  }
}
