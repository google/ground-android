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
package org.groundplatform.feature.pdf.render.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.groundplatform.feature.pdf.render.PdfConfig
import org.groundplatform.feature.pdf.render.PdfOffset
import org.groundplatform.feature.pdf.render.layout.PageHeaderLayout.Companion.BOTTOM_GAP
import org.groundplatform.feature.pdf.render.layout.PageHeaderLayout.Companion.COLUMN_GAP

class PageHeaderLayoutTest {

  private val lineSpacing = PdfConfig.LINE_SPACING
  private val headerBottomGap = BOTTOM_GAP
  private val headerColumnGap = COLUMN_GAP
  private val usableWidth = PdfConfig.USABLE_WIDTH

  @Test
  fun `column X positions span the usable width with gaps between them`() {
    val left = PageHeaderLayout.LEFT_X
    val center = PageHeaderLayout.CENTER_X
    val right = PageHeaderLayout.RIGHT_X
    val width = PageHeaderLayout.COLUMN_WIDTH

    assertTrue(left < center)
    assertTrue(center < right)
    assertEquals(headerColumnGap.toFloat(), center - (left + width))
    assertEquals(headerColumnGap.toFloat(), right - (center + width))
    assertTrue(3 * width + 2 * headerColumnGap <= usableWidth)
  }

  @Test
  fun `compute places survey column labels and values at LEFT_X`() {
    val layout = PageHeaderLayout.compute(top = 0f, labelHeight = 10f, valueHeight = 14f)

    assertEquals(PdfOffset(PageHeaderLayout.LEFT_X, 0f), layout.leftColumn.labelOffset)
    assertEquals(
      PdfOffset(PageHeaderLayout.LEFT_X, 10f + lineSpacing),
      layout.leftColumn.valueOffset,
    )
  }

  @Test
  fun `compute places job column labels and values at CENTER_X`() {
    val layout = PageHeaderLayout.compute(top = 0f, labelHeight = 10f, valueHeight = 14f)

    assertEquals(PdfOffset(PageHeaderLayout.CENTER_X, 0f), layout.centerColumn.labelOffset)
    assertEquals(
      PdfOffset(PageHeaderLayout.CENTER_X, 10f + lineSpacing),
      layout.centerColumn.valueOffset,
    )
  }

  @Test
  fun `compute places timestamp at RIGHT_X with the same top as labels`() {
    val layout = PageHeaderLayout.compute(top = 50f, labelHeight = 10f, valueHeight = 14f)

    assertEquals(PdfOffset(PageHeaderLayout.RIGHT_X, 50f), layout.rightTextOffset)
    assertEquals(layout.leftColumn.labelOffset.y, layout.rightTextOffset.y)
    assertEquals(layout.centerColumn.labelOffset.y, layout.rightTextOffset.y)
  }

  @Test
  fun `value sits below its label by exactly line spacing`() {
    val labelHeight = 12f
    val layout = PageHeaderLayout.compute(top = 30f, labelHeight = labelHeight, valueHeight = 14f)

    val survey = layout.leftColumn
    assertEquals(labelHeight + lineSpacing, survey.valueOffset.y - survey.labelOffset.y)
  }

  @Test
  fun `nextCursorY accounts for label, line spacing, value, and header bottom gap`() {
    val top = 40f
    val labelHeight = 10f
    val valueHeight = 14f

    val layout = PageHeaderLayout.compute(top = top, labelHeight, valueHeight)

    assertEquals(
      top + labelHeight + lineSpacing + valueHeight + headerBottomGap,
      layout.nextCursorY,
    )
  }

  @Test
  fun `all three columns share the same label baseline and value baseline`() {
    val layout = PageHeaderLayout.compute(top = 100f, labelHeight = 12f, valueHeight = 16f)

    assertEquals(layout.leftColumn.labelOffset.y, layout.centerColumn.labelOffset.y)
    assertEquals(layout.leftColumn.valueOffset.y, layout.centerColumn.valueOffset.y)
  }
}
