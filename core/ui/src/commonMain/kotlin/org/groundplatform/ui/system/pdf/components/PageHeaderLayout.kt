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

import org.groundplatform.ui.system.pdf.PdfConfig.HEADER_BOTTOM_GAP
import org.groundplatform.ui.system.pdf.PdfConfig.HEADER_COLUMN_GAP
import org.groundplatform.ui.system.pdf.PdfConfig.LINE_SPACING
import org.groundplatform.ui.system.pdf.PdfConfig.MARGIN
import org.groundplatform.ui.system.pdf.PdfConfig.USABLE_WIDTH
import org.groundplatform.ui.system.pdf.PdfRenderer

/**
 * Pre-computed layout for the page header with three slots. Assumes uniform typography across
 * columns.
 *
 * @param leftColumn Label and value positions for the left column.
 * @param centerColumn Label and value positions for the center column.
 * @param rightTextOffset The position where the right-aligned value begins .
 * @param nextCursorY The Y position where the cursor should be positioned after the header.
 */
internal data class PageHeaderLayout(
  val leftColumn: Column,
  val centerColumn: Column,
  val rightTextOffset: PdfRenderer.Offset,
  val nextCursorY: Float,
) {
  companion object {
    const val COLUMN_WIDTH: Int = (USABLE_WIDTH - 2 * HEADER_COLUMN_GAP) / 3
    const val LEFT_X: Float = MARGIN.toFloat()
    const val CENTER_X: Float = LEFT_X + COLUMN_WIDTH + HEADER_COLUMN_GAP
    const val RIGHT_X: Float = LEFT_X + 2 * (COLUMN_WIDTH + HEADER_COLUMN_GAP)

    fun compute(top: Float, labelHeight: Float, valueHeight: Float): PageHeaderLayout {
      val columnBottom = top + labelHeight + LINE_SPACING + valueHeight
      return PageHeaderLayout(
        leftColumn = column(LEFT_X, top, labelHeight),
        centerColumn = column(CENTER_X, top, labelHeight),
        rightTextOffset = PdfRenderer.Offset(RIGHT_X, top),
        nextCursorY = columnBottom + HEADER_BOTTOM_GAP,
      )
    }

    private fun column(x: Float, top: Float, labelHeight: Float) =
      Column(
        labelOffset = PdfRenderer.Offset(x, top),
        valueOffset = PdfRenderer.Offset(x, top + labelHeight + LINE_SPACING),
      )
  }

  data class Column(val labelOffset: PdfRenderer.Offset, val valueOffset: PdfRenderer.Offset)
}
