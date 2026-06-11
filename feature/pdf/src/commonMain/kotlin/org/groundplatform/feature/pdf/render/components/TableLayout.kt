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

import org.groundplatform.feature.pdf.render.PdfConfig.CELL_PADDING
import org.groundplatform.feature.pdf.render.PdfConfig.LINE_SPACING
import org.groundplatform.feature.pdf.render.PdfConfig.MARGIN
import org.groundplatform.feature.pdf.render.PdfConfig.TABLE_TASK_COLUMN_WIDTH
import org.groundplatform.feature.pdf.render.PdfConfig.USABLE_WIDTH
import org.groundplatform.feature.pdf.render.PdfItemSize
import org.groundplatform.feature.pdf.render.PdfLine
import org.groundplatform.feature.pdf.render.PdfOffset
import org.groundplatform.feature.pdf.render.PdfRect

internal object TableLayout {
  /**
   * Layout of a single two-column table row. Left cell holds a single text block; right cell may
   * contain either text or an image.
   *
   * @param totalHeight Total height of the row including vertical padding.
   * @property content Content-specific layout data.
   * @property borders All the borders to draw around the row.
   */
  data class Row(val totalHeight: Float, val content: Content, val borders: Borders) {
    /**
     * @param top Horizontal line along the row's top edge, or null if there is already a row
     *   directly above it on the same page.
     * @param bottom Horizontal line along the row's bottom edge.
     * @param divider Vertical line between the two columns, spanning the row's full height.
     */
    data class Borders(val top: PdfLine?, val divider: PdfLine, val bottom: PdfLine) {
      val drawableLines: List<PdfLine>
        get() = listOfNotNull(top, divider, bottom)
    }

    /**
     * @param leftTextOffset Top-left position where the left cell text should be drawn.
     * @param rightTextOffset Top-left position where the right cell text should be drawn, or null
     *   if the right cell has no text.
     * @param rightImageFrame Frame (x, y, width, height) for the right cell image, or null if no
     *   image.
     */
    data class Content(
      val leftTextOffset: PdfOffset,
      val rightTextOffset: PdfOffset?,
      val rightImageFrame: PdfRect?,
    )
  }

  data class Label(val labelOffset: PdfOffset, val nextCursorY: Float)

  fun getLabel(top: Float, labelHeight: Float): Label {
    val labelTop = top + LINE_SPACING * 2
    return Label(
      labelOffset = PdfOffset(MARGIN.toFloat(), labelTop),
      nextCursorY = labelTop + labelHeight + LINE_SPACING,
    )
  }

  /** Row height for the page-fit check, before the final row position is known. */
  fun getRowHeight(
    leftTextHeight: Float,
    rightTextHeight: Float,
    rightImageSize: PdfItemSize?,
  ): Float {
    val imageHeight = rightImageSize?.height ?: 0f
    val imageSpacing = if (imageHeight > 0f && rightTextHeight > 0f) LINE_SPACING else 0f
    return maxOf(leftTextHeight, rightTextHeight + imageHeight + imageSpacing) + 2 * CELL_PADDING
  }

  fun getRow(
    rowTop: Float,
    leftTextHeight: Float,
    rightTextHeight: Float,
    rightImageSize: PdfItemSize?,
    includeTopBorder: Boolean = true,
  ): Row {
    val totalHeight = getRowHeight(leftTextHeight, rightTextHeight, rightImageSize)

    val left = MARGIN.toFloat()
    val right = left + USABLE_WIDTH
    val midX = left + TABLE_TASK_COLUMN_WIDTH
    val rowBottom = rowTop + totalHeight
    val contentTop = rowTop + CELL_PADDING
    val rightCellLeft = midX + CELL_PADDING

    val rightTextOffset = if (rightTextHeight > 0f) PdfOffset(rightCellLeft, contentTop) else null
    val rightImageFrame = rightImageSize?.let {
      val y = contentTop + (rightTextOffset?.let { rightTextHeight + LINE_SPACING } ?: 0f)
      PdfRect(rightCellLeft, y, it.width, it.height)
    }

    return Row(
      totalHeight = totalHeight,
      content =
        Row.Content(
          leftTextOffset = PdfOffset(left + CELL_PADDING, contentTop),
          rightTextOffset = rightTextOffset,
          rightImageFrame = rightImageFrame,
        ),
      borders =
        Row.Borders(
          top =
            if (includeTopBorder)
              PdfLine(startX = left, startY = rowTop, endX = right, endY = rowTop)
            else null,
          divider = PdfLine(startX = midX, startY = rowTop, endX = midX, endY = rowBottom),
          bottom = PdfLine(startX = left, startY = rowBottom, endX = right, endY = rowBottom),
        ),
    )
  }
}
