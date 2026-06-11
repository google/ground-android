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

import org.groundplatform.feature.pdf.render.PdfConfig.LINE_SPACING
import org.groundplatform.feature.pdf.render.PdfConfig.MARGIN
import org.groundplatform.feature.pdf.render.PdfConfig.USABLE_WIDTH
import org.groundplatform.feature.pdf.render.PdfOffset

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
  val rightTextOffset: PdfOffset,
  val nextCursorY: Float,
) {
  companion object {
    /** Horizontal gap between the two columns of the page header. */
    const val COLUMN_GAP = 16
    /** Vertical gap below the header before body content begins. */
    const val BOTTOM_GAP = 28f

    /** Maximum number of lines rendered for each header value. */
    const val MAX_LINES = 1
    const val COLUMN_WIDTH: Int = (USABLE_WIDTH - 2 * COLUMN_GAP) / 3
    const val LEFT_X: Float = MARGIN.toFloat()
    const val CENTER_X: Float = LEFT_X + COLUMN_WIDTH + COLUMN_GAP
    const val RIGHT_X: Float = LEFT_X + 2 * (COLUMN_WIDTH + COLUMN_GAP)

    fun compute(top: Float, labelHeight: Float, valueHeight: Float): PageHeaderLayout {
      val columnBottom = top + labelHeight + LINE_SPACING + valueHeight
      return PageHeaderLayout(
        leftColumn = column(LEFT_X, top, labelHeight),
        centerColumn = column(CENTER_X, top, labelHeight),
        rightTextOffset = PdfOffset(RIGHT_X, top),
        nextCursorY = columnBottom + BOTTOM_GAP,
      )
    }

    private fun column(x: Float, top: Float, labelHeight: Float) =
      Column(
        labelOffset = PdfOffset(x, top),
        valueOffset = PdfOffset(x, top + labelHeight + LINE_SPACING),
      )
  }

  data class Column(val labelOffset: PdfOffset, val valueOffset: PdfOffset)
}
