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
package org.groundplatform.feature.pdf.render.layout.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.groundplatform.feature.pdf.render.PdfRect

class NorthArrowLayoutTest {

  @Test
  fun `label is centered on the arrow axis, inset from the frame's top-right corner`() {
    assertEquals(379.5f, layout.labelOffset.x)
    assertEquals(200f, layout.labelOffset.y)
  }

  @Test
  fun `arrow is a triangle pointing up, symmetric about the arrow axis`() {
    val (tip, rightBase, leftBase) = layout.arrow

    assertEquals(386.5f, tip.x)
    assertEquals(210f, tip.y)
    assertEquals(392f, rightBase.x)
    assertEquals(381f, leftBase.x)
    assertEquals(230f, rightBase.y)
    assertEquals(230f, leftBase.y)
  }

  @Test
  fun `the whole arrow block stays inside the plot image`() {
    val points = layout.arrow + layout.labelOffset

    assertTrue(points.all { it.x >= frame.x && it.x <= frame.right })
    assertTrue(points.all { it.y >= frame.y && it.y <= frame.bottom })
    assertTrue(layout.labelOffset.x + NorthArrowLayout.LABEL_WIDTH <= frame.right)
  }

  @Test
  fun `height covers the label, gap, and arrow`() {
    val top = layout.labelOffset.y
    val bottom = layout.arrow.maxOf { it.y }

    assertEquals(NorthArrowLayout.height(LABEL_HEIGHT), bottom - top)
  }

  private companion object {
    const val LABEL_HEIGHT = 9f
    private val frame = PdfRect(x = 100f, y = 192f, width = 300f, height = 300f)
    private val layout = NorthArrowLayout.compute(frame, LABEL_HEIGHT)
  }
}
