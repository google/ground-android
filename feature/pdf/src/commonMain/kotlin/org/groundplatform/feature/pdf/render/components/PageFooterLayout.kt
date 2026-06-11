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

import org.groundplatform.feature.pdf.render.PdfConfig.MARGIN
import org.groundplatform.feature.pdf.render.PdfConfig.PAGE_HEIGHT
import org.groundplatform.feature.pdf.render.PdfConfig.USABLE_WIDTH
import org.groundplatform.feature.pdf.render.PdfOffset

/**
 * Pre-computed layout for the page footer with separate left and right slots.
 *
 * @param footerTextOffset The top-left position where the footer text begins.
 * @param pageNumberOffset The top-left position where the page number begins.
 * @param pageNumberMaxWidth The maximum width available for the page number
 */
internal data class PageFooterLayout(
  val footerTextOffset: PdfOffset,
  val pageNumberOffset: PdfOffset,
  val pageNumberMaxWidth: Int,
) {
  companion object {
    /** Vertical gap above the footer separating it from body content. */
    const val TOP_GAP = 28f

    /** Width reserved on the footer's right side for the page-number label. */
    const val PAGE_NUMBER_BAND_WIDTH = 60

    /** Minimum horizontal gap between the footer text and the page number. */
    const val PAGE_NUMBER_GAP = 8

    /** Maximum width for footer text (usable width minus the page-number band and its gap). */
    const val TEXT_MAX_WIDTH = USABLE_WIDTH - PAGE_NUMBER_BAND_WIDTH - PAGE_NUMBER_GAP

    /** Maximum number of lines rendered for the footer text. */
    const val MAX_LINES = 1

    /**
     * Vertical space the footer occupies, including the [TOP_GAP] separating it from page content.
     */
    fun reserve(footerHeight: Float): Float = footerHeight + TOP_GAP

    fun compute(footerHeight: Float): PageFooterLayout {
      val top = PAGE_HEIGHT - MARGIN - footerHeight
      val left = MARGIN.toFloat()
      val pageNumberLeft = left + USABLE_WIDTH - PAGE_NUMBER_BAND_WIDTH
      return PageFooterLayout(
        footerTextOffset = PdfOffset(left, top),
        pageNumberOffset = PdfOffset(pageNumberLeft, top),
        pageNumberMaxWidth = PAGE_NUMBER_BAND_WIDTH,
      )
    }
  }
}
