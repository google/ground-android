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
package org.groundplatform.feature.pdf.render

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class PdfGeometryTest {

  @Test
  fun `fitInside scales down to fit the width constraint`() {
    val result = fitInside(width = 200, height = 100, maxWidth = 100, maxHeight = 100)

    assertEquals(100f, result.width)
    assertEquals(50f, result.height)
  }

  @Test
  fun `fitInside scales down to fit the height constraint`() {
    val result = fitInside(width = 100, height = 200, maxWidth = 100, maxHeight = 100)

    assertEquals(50f, result.width)
    assertEquals(100f, result.height)
  }

  @Test
  fun `fitInside preserves aspect ratio`() {
    val result = fitInside(width = 400, height = 300, maxWidth = 200, maxHeight = 200)

    assertEquals(200f, result.width)
    assertEquals(150f, result.height)
    assertEquals(result.width / result.height, 400f / 300f)
  }

  @Test
  fun `fitInside never upscales when the item already fits`() {
    val result = fitInside(width = 50, height = 30, maxWidth = 100, maxHeight = 100)

    assertEquals(50f, result.width)
    assertEquals(30f, result.height)
  }

  @Test
  fun `fitInside returns the same size when dimensions equal the bounds`() {
    val result = fitInside(width = 100, height = 100, maxWidth = 100, maxHeight = 100)

    assertEquals(100f, result.width)
    assertEquals(100f, result.height)
  }

  @Test
  fun `pointsToRenderPixels converts points to pixels at the configured DPI`() {
    // 72 points = 1 inch, which at IMAGE_RENDER_DPI yields exactly that many pixels.
    assertEquals(PdfConfig.IMAGE_RENDER_DPI.roundToInt(), pointsToRenderPixels(72f))
  }

  @Test
  fun `pointsToRenderPixels scales linearly and rounds to the nearest pixel`() {
    assertEquals(0, pointsToRenderPixels(0f))
    assertEquals((36f / 72f * PdfConfig.IMAGE_RENDER_DPI).roundToInt(), pointsToRenderPixels(36f))
    assertEquals((10f / 72f * PdfConfig.IMAGE_RENDER_DPI).roundToInt(), pointsToRenderPixels(10f))
  }

  @Test
  fun `PdfRect exposes right and bottom derived from origin and size`() {
    val rect = PdfRect(x = 10f, y = 20f, width = 30f, height = 40f)

    assertEquals(40f, rect.right)
    assertEquals(60f, rect.bottom)
  }
}
