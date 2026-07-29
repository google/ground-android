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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.groundplatform.feature.pdf.render.PdfRect

class ScaleBarLayoutTest {

  @Test
  fun `line is horizontal and spans the given width`() {
    assertEquals(layout.line.startY, layout.line.endY)
    assertEquals(BAR_WIDTH, layout.line.endX - layout.line.startX)
  }

  @Test
  fun `line is inset from the frame's bottom-right corner`() {
    assertEquals(332f, layout.line.startX)
    assertEquals(392f, layout.line.endX)
    assertEquals(492f, layout.line.endY)
  }

  @Test
  fun `label sits directly above the line, aligned with its left end`() {
    assertEquals(332f, layout.labelOffset.x)
    assertEquals(481f, layout.labelOffset.y)
  }

  @Test
  fun `the whole line scale block stays inside the plot image`() {
    assertTrue(layout.labelOffset.y >= frame.y)
    assertTrue(layout.line.startX >= frame.x)
    assertTrue(layout.line.endX <= frame.right)
    assertTrue(layout.line.endY <= frame.bottom)
  }

  @Test
  fun `measure picks a round distance that exactly fills the quarter-width a bar may occupy`() {
    // 400 m over 400 points is 1 m per point, so the 100-point quarter is exactly 100 m.
    val bar = ScaleBarLayout.measure(groundWidthMeters = 400.0, imageWidthPoints = 400f)

    assertEquals("100 m", bar?.label)
    assertEquals(100f, bar?.lengthPoints)
  }

  @Test
  fun `measure rounds the distance down so the bar fits within that quarter`() {
    // 1 m per point again, but 170 points to play with: 100 m is the longest round distance
    // fitting.
    val bar = ScaleBarLayout.measure(groundWidthMeters = 680.0, imageWidthPoints = 680f)

    assertEquals("100 m", bar?.label)
    assertEquals(100f, bar?.lengthPoints)
  }

  @Test
  fun `measure uses the 2 and 5 steps between powers of ten`() {
    // 1 m per point over a 250-point quarter rounds to 200 m; 2.4 m per point over the same
    // quarter gives 600 m of room, which rounds to 500 m.
    assertEquals(
      "200 m",
      ScaleBarLayout.measure(groundWidthMeters = 1000.0, imageWidthPoints = 1000f)?.label,
    )
    assertEquals(
      "500 m",
      ScaleBarLayout.measure(groundWidthMeters = 2400.0, imageWidthPoints = 1000f)?.label,
    )
  }

  @Test
  fun `measure labels distances of a kilometre and up in kilometres`() {
    // 10 m per point over a 250-point quarter: 2000 m fits and reads as "2 km".
    val bar = ScaleBarLayout.measure(groundWidthMeters = 10_000.0, imageWidthPoints = 1000f)

    assertEquals("2 km", bar?.label)
    assertEquals(200f, bar?.lengthPoints)
  }

  @Test
  fun `measure never returns a bar wider than a quarter of the image`() {
    val imageWidth = 515f
    val groundWidths = listOf(40.0, 137.0, 999.0, 5_000.0, 87_000.0)

    groundWidths.forEach { groundWidth ->
      val bar = ScaleBarLayout.measure(groundWidth, imageWidth)

      assertTrue(bar != null && bar.lengthPoints <= imageWidth / 4f, "overran for $groundWidth m")
    }
  }

  @Test
  fun `measure returns null for non-positive inputs`() {
    assertNull(ScaleBarLayout.measure(groundWidthMeters = 0.0, imageWidthPoints = 400f))
    assertNull(ScaleBarLayout.measure(groundWidthMeters = 400.0, imageWidthPoints = 0f))
  }

  private companion object {
    val frame = PdfRect(x = 100f, y = 200f, width = 300f, height = 300f)
    const val BAR_WIDTH = 60f
    const val LABEL_HEIGHT = 10f

    val layout = ScaleBarLayout.compute(frame, BAR_WIDTH, LABEL_HEIGHT)
  }
}
