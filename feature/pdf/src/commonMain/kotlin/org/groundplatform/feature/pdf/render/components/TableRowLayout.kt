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
import org.groundplatform.feature.pdf.render.PdfOffset
import org.groundplatform.feature.pdf.render.PdfRect

/**
 * Pre-computed layout for a two-column table row. Left cell holds a single text block; right cell
 * may contain either text or an image.
 *
 * @param totalHeight Total height of the row including vertical padding.
 * @param leftRowX X coordinate of the row's left edge.
 * @param rightRowX X coordinate of the row's right edge.
 * @param columnDividerX X coordinate of the vertical divider between the two columns.
 * @param leftTextOffset Top-left position where the left cell text should be drawn.
 * @param rightTextOffset Top-left position where the right cell text should be drawn, or null if
 *   the right cell has no text.
 * @param rightImageFrame Frame (x, y, width, height) for the right cell image, or null if no image.
 */
internal data class TableRowLayout(
  val totalHeight: Float,
  val leftRowX: Float,
  val rightRowX: Float,
  val columnDividerX: Float,
  val leftTextOffset: PdfOffset,
  val rightTextOffset: PdfOffset?,
  val rightImageFrame: PdfRect?,
) {
  companion object {
    fun totalHeight(
      leftTextHeight: Float,
      rightTextHeight: Float,
      rightImageSize: PdfItemSize?,
    ): Float {
      val imageHeight = rightImageSize?.height ?: 0f
      val imageSpacing = if (imageHeight > 0f && rightTextHeight > 0f) LINE_SPACING else 0f
      return maxOf(leftTextHeight, rightTextHeight + imageHeight + imageSpacing) + 2 * CELL_PADDING
    }

    fun compute(
      rowTop: Float,
      leftTextHeight: Float,
      rightTextHeight: Float,
      rightImageSize: PdfItemSize?,
    ): TableRowLayout {
      val totalHeight = totalHeight(leftTextHeight, rightTextHeight, rightImageSize)

      val left = MARGIN.toFloat()
      val midX = left + TABLE_TASK_COLUMN_WIDTH
      val contentTop = rowTop + CELL_PADDING
      val rightCellLeft = midX + CELL_PADDING

      val rightTextOffset = if (rightTextHeight > 0f) PdfOffset(rightCellLeft, contentTop) else null
      val rightImageFrame = rightImageSize?.let {
        val y = contentTop + (rightTextOffset?.let { rightTextHeight + LINE_SPACING } ?: 0f)
        PdfRect(rightCellLeft, y, rightImageSize.width, rightImageSize.height)
      }

      return TableRowLayout(
        totalHeight = totalHeight,
        leftRowX = left,
        rightRowX = left + USABLE_WIDTH,
        columnDividerX = midX,
        leftTextOffset = PdfOffset(left + CELL_PADDING, contentTop),
        rightTextOffset = rightTextOffset,
        rightImageFrame = rightImageFrame,
      )
    }
  }
}
