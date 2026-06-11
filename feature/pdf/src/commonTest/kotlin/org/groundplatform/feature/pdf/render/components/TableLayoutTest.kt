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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.groundplatform.feature.pdf.render.PdfConfig
import org.groundplatform.feature.pdf.render.PdfItemSize
import org.groundplatform.feature.pdf.render.PdfLine
import org.groundplatform.feature.pdf.render.PdfOffset
import org.groundplatform.feature.pdf.render.components.TableLayout.CELL_PADDING
import org.groundplatform.feature.pdf.render.components.TableLayout.TASK_COLUMN_WIDTH

class TableLayoutTest {

  private val cellPadding = CELL_PADDING.toFloat()
  private val lineSpacing = PdfConfig.LINE_SPACING
  private val margin = PdfConfig.MARGIN.toFloat()
  private val usableWidth = PdfConfig.USABLE_WIDTH
  private val taskColumnWidth = TASK_COLUMN_WIDTH

  @Test
  fun `label sits below a top gap at the left margin`() {
    val layout = TableLayout.getLabel(top = 100f, labelHeight = 14f)

    assertEquals(PdfOffset(margin, 100f + 2 * lineSpacing), layout.labelOffset)
  }

  @Test
  fun `label leaves a bottom gap before the first row`() {
    val top = 100f
    val labelHeight = 14f

    val layout = TableLayout.getLabel(top = top, labelHeight = labelHeight)

    assertEquals(top + 2 * lineSpacing + labelHeight + lineSpacing, layout.nextCursorY)
  }

  @Test
  fun `taller label pushes the first row further down`() {
    val short = TableLayout.getLabel(top = 0f, labelHeight = 10f)
    val tall = TableLayout.getLabel(top = 0f, labelHeight = 30f)

    assertTrue(short.nextCursorY < tall.nextCursorY)
    assertEquals(20f, tall.nextCursorY - short.nextCursorY)
  }

  @Test
  fun `rowHeight with only left text returns left height plus padding`() {
    val height =
      TableLayout.getRowHeight(leftTextHeight = 30f, rightTextHeight = 0f, rightImageSize = null)

    assertEquals(30f + 2 * cellPadding, height)
  }

  @Test
  fun `rowHeight picks the taller content height`() {
    val tallerLeft =
      TableLayout.getRowHeight(leftTextHeight = 50f, rightTextHeight = 20f, rightImageSize = null)
    val tallerRight =
      TableLayout.getRowHeight(leftTextHeight = 10f, rightTextHeight = 60f, rightImageSize = null)
    val tallerImageRight =
      TableLayout.getRowHeight(
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = PdfItemSize(width = 100f, height = 80f),
      )

    assertEquals(50f + 2 * cellPadding, tallerLeft)
    assertEquals(60f + 2 * cellPadding, tallerRight)
    assertEquals(80f + 2 * cellPadding, tallerImageRight)
  }

  @Test
  fun `rowHeight with both right text and image stacks them with line spacing`() {
    val height =
      TableLayout.getRowHeight(
        leftTextHeight = 10f,
        rightTextHeight = 20f,
        rightImageSize = PdfItemSize(width = 100f, height = 80f),
      )

    assertEquals(20f + lineSpacing + 80f + 2 * cellPadding, height)
  }

  @Test
  fun `row always places left text at the row's top-left content area`() {
    val layout =
      TableLayout.getRow(
        rowTop = 100f,
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = null,
      )

    assertEquals(PdfOffset(margin + cellPadding, 100f + cellPadding), layout.content.leftTextOffset)
  }

  @Test
  fun `row returns null right offsets when right cell has no content`() {
    val layout =
      TableLayout.getRow(
        rowTop = 0f,
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = null,
      )

    assertNull(layout.content.rightTextOffset)
    assertNull(layout.content.rightImageFrame)
  }

  @Test
  fun `row places right text at the right cell's top`() {
    val layout =
      TableLayout.getRow(
        rowTop = 50f,
        leftTextHeight = 20f,
        rightTextHeight = 20f,
        rightImageSize = null,
      )

    val rightCellX = margin + taskColumnWidth + cellPadding
    assertEquals(PdfOffset(rightCellX, 50f + cellPadding), layout.content.rightTextOffset)
    assertNull(layout.content.rightImageFrame)
  }

  @Test
  fun `row places image at the right cell's top`() {
    val imageSize = PdfItemSize(width = 80f, height = 60f)
    val layout =
      TableLayout.getRow(
        rowTop = 50f,
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = imageSize,
      )

    val rightCellX = margin + taskColumnWidth + cellPadding
    val frame = assertNotNull(layout.content.rightImageFrame)
    assertNull(layout.content.rightTextOffset)
    with(frame) {
      assertEquals(rightCellX, x)
      assertEquals(50f + cellPadding, y)
      assertEquals(imageSize.width, width)
      assertEquals(imageSize.height, height)
    }
  }

  @Test
  fun `row sets bounds and divider from page geometry`() {
    val layout =
      TableLayout.getRow(
        rowTop = 0f,
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = null,
      )

    val leftX = layout.borders.bottom.startX
    val rightX = layout.borders.bottom.endX
    val dividerX = layout.borders.divider.startX
    assertEquals(margin, leftX)
    assertEquals(margin + usableWidth, rightX)
    assertEquals(margin + taskColumnWidth, dividerX)
    assertTrue(leftX < dividerX)
    assertTrue(dividerX < rightX)
  }

  @Test
  fun `row frames itself with top, bottom, and column-divider border lines`() {
    val rowTop = 100f
    val layout =
      TableLayout.getRow(
        rowTop = rowTop,
        leftTextHeight = 20f,
        rightTextHeight = 20f,
        rightImageSize = null,
      )

    val rowBottom = rowTop + layout.totalHeight
    val right = margin + usableWidth
    val midX = margin + taskColumnWidth
    val borders = layout.borders
    assertEquals(PdfLine(margin, rowTop, right, rowTop), borders.top)
    assertEquals(PdfLine(margin, rowBottom, right, rowBottom), borders.bottom)
    assertEquals(PdfLine(midX, rowTop, midX, rowBottom), borders.divider)
    assertEquals(listOf(borders.top, borders.divider, borders.bottom), borders.drawableLines)
  }

  @Test
  fun `row omits its top border when drawTopBorder is false`() {
    val first =
      TableLayout.getRow(
        rowTop = 50f,
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = null,
      )
    val second =
      TableLayout.getRow(
        rowTop = 50f + first.totalHeight,
        leftTextHeight = 30f,
        rightTextHeight = 0f,
        rightImageSize = null,
        includeTopBorder = false,
      )

    assertNull(second.borders.top)
    assertEquals(first.borders.bottom.startY, second.borders.divider.startY)
    assertEquals(first.borders.divider.endX, second.borders.divider.startX)
    assertEquals(first.borders.divider.endY, second.borders.divider.startY)
  }

  @Test
  fun `consecutive rows share borders so the divider reads as one continuous line`() {
    val first =
      TableLayout.getRow(
        rowTop = 50f,
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = null,
      )
    val second =
      TableLayout.getRow(
        rowTop = 50f + first.totalHeight,
        leftTextHeight = 30f,
        rightTextHeight = 0f,
        rightImageSize = null,
        includeTopBorder = false,
      )

    assertNull(second.borders.top)
    assertEquals(first.borders.bottom.startY, second.borders.divider.startY)
    assertEquals(first.borders.divider.endX, second.borders.divider.startX)
    assertEquals(first.borders.divider.endY, second.borders.divider.startY)
  }

  @Test
  fun `row totalHeight matches the rowHeight helper`() {
    val left = 30f
    val right = 20f
    val image = PdfItemSize(width = 80f, height = 60f)
    val layout = TableLayout.getRow(rowTop = 0f, left, right, image)

    assertEquals(TableLayout.getRowHeight(left, right, image), layout.totalHeight)
  }
}
