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
import org.groundplatform.feature.pdf.render.PdfOffset

class TableRowLayoutTest {

  private val cellPadding = PdfConfig.CELL_PADDING.toFloat()
  private val lineSpacing = PdfConfig.LINE_SPACING
  private val margin = PdfConfig.MARGIN.toFloat()
  private val usableWidth = PdfConfig.USABLE_WIDTH
  private val taskColumnWidth = PdfConfig.TABLE_TASK_COLUMN_WIDTH

  @Test
  fun `totalHeight with only left text returns left height plus padding`() {
    val height =
      TableRowLayout.totalHeight(leftTextHeight = 30f, rightTextHeight = 0f, rightImageSize = null)

    assertEquals(30f + 2 * cellPadding, height)
  }

  @Test
  fun `totalHeight picks the taller content height`() {
    val tallerLeft =
      TableRowLayout.totalHeight(leftTextHeight = 50f, rightTextHeight = 20f, rightImageSize = null)
    val tallerRight =
      TableRowLayout.totalHeight(leftTextHeight = 10f, rightTextHeight = 60f, rightImageSize = null)
    val tallerImageRight =
      TableRowLayout.totalHeight(
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = PdfItemSize(width = 100f, height = 80f),
      )

    assertEquals(50f + 2 * cellPadding, tallerLeft)
    assertEquals(60f + 2 * cellPadding, tallerRight)
    assertEquals(80f + 2 * cellPadding, tallerImageRight)
  }

  @Test
  fun `totalHeight with both right text and image stacks them with line spacing`() {
    val height =
      TableRowLayout.totalHeight(
        leftTextHeight = 10f,
        rightTextHeight = 20f,
        rightImageSize = PdfItemSize(width = 100f, height = 80f),
      )

    assertEquals(20f + lineSpacing + 80f + 2 * cellPadding, height)
  }

  @Test
  fun `compute always places left text at the row's top-left content area`() {
    val layout =
      TableRowLayout.compute(
        rowTop = 100f,
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = null,
      )

    assertEquals(PdfOffset(margin + cellPadding, 100f + cellPadding), layout.leftTextOffset)
  }

  @Test
  fun `compute returns null right offsets when right cell has no content`() {
    val layout =
      TableRowLayout.compute(
        rowTop = 0f,
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = null,
      )

    assertNull(layout.rightTextOffset)
    assertNull(layout.rightImageFrame)
  }

  @Test
  fun `compute places right text at the right cell's top`() {
    val layout =
      TableRowLayout.compute(
        rowTop = 50f,
        leftTextHeight = 20f,
        rightTextHeight = 20f,
        rightImageSize = null,
      )

    val rightCellX = margin + taskColumnWidth + cellPadding
    assertEquals(PdfOffset(rightCellX, 50f + cellPadding), layout.rightTextOffset)
    assertNull(layout.rightImageFrame)
  }

  @Test
  fun `compute places image at the right cell's top`() {
    val imageSize = PdfItemSize(width = 80f, height = 60f)
    val layout =
      TableRowLayout.compute(
        rowTop = 50f,
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = imageSize,
      )

    val rightCellX = margin + taskColumnWidth + cellPadding
    val frame = assertNotNull(layout.rightImageFrame)
    assertNull(layout.rightTextOffset)
    with(frame) {
      assertEquals(rightCellX, x)
      assertEquals(50f + cellPadding, y)
      assertEquals(imageSize.width, width)
      assertEquals(imageSize.height, height)
    }
  }

  @Test
  fun `compute sets row bounds and divider from page geometry`() {
    val layout =
      TableRowLayout.compute(
        rowTop = 0f,
        leftTextHeight = 20f,
        rightTextHeight = 0f,
        rightImageSize = null,
      )

    assertEquals(margin, layout.leftRowX)
    assertEquals(margin + usableWidth, layout.rightRowX)
    assertEquals(margin + taskColumnWidth, layout.columnDividerX)
    assertTrue(layout.leftRowX < layout.columnDividerX)
    assertTrue(layout.columnDividerX < layout.rightRowX)
  }

  @Test
  fun `compute totalHeight matches the static helper`() {
    val left = 30f
    val right = 20f
    val image = PdfItemSize(width = 80f, height = 60f)
    val layout = TableRowLayout.compute(rowTop = 0f, left, right, image)

    assertEquals(TableRowLayout.totalHeight(left, right, image), layout.totalHeight)
  }
}
