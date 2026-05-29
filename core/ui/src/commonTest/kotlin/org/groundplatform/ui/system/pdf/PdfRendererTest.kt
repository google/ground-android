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
package org.groundplatform.ui.system.pdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfRendererTest {

  @Test
  fun `fitInside returns the input when it already fits inside the bounds`() {
    val size = PdfRenderer.fitInside(width = 50, height = 30, maxWidth = 100, maxHeight = 80)

    assertEquals(50f, size.width)
    assertEquals(30f, size.height)
  }

  @Test
  fun `fitInside never upscales when input is smaller than the bounds`() {
    val size = PdfRenderer.fitInside(width = 10, height = 10, maxWidth = 1000, maxHeight = 1000)

    assertEquals(10f, size.width)
    assertEquals(10f, size.height)
  }

  @Test
  fun `fitInside scales down by width when width is the binding axis`() {
    val size = PdfRenderer.fitInside(width = 200, height = 50, maxWidth = 100, maxHeight = 100)

    assertEquals(100f, size.width)
    assertEquals(25f, size.height)
  }

  @Test
  fun `fitInside scales down by height when height is the binding axis`() {
    val size = PdfRenderer.fitInside(width = 50, height = 200, maxWidth = 100, maxHeight = 100)

    assertEquals(25f, size.width)
    assertEquals(100f, size.height)
  }

  @Test
  fun `fitInside preserves the input aspect ratio`() {
    val inputAspect = 200f / 80f
    val size = PdfRenderer.fitInside(width = 200, height = 80, maxWidth = 50, maxHeight = 50)
    val outputAspect = size.width / size.height

    assertEquals(inputAspect, outputAspect, 0.0001f)
  }

  @Test
  fun `fitInside fits exactly when input matches the bounds`() {
    val size = PdfRenderer.fitInside(width = 100, height = 80, maxWidth = 100, maxHeight = 80)

    assertEquals(100f, size.width)
    assertEquals(80f, size.height)
  }

  @Test
  fun `pointsToRenderPixels converts one inch correctly`() {
    val onePointInRenderPx = PdfRenderer.pointsToRenderPixels(72f) // 72pt = 1 inch

    assertEquals(PdfConfig.IMAGE_RENDER_DPI.toInt(), onePointInRenderPx)
  }

  @Test
  fun `pointsToRenderPixels rounds to the nearest pixel`() {
    // 1 / 72 * 300 DPI = 4.166
    assertEquals(4, PdfRenderer.pointsToRenderPixels(1f))
  }

  @Test
  fun `larger point values produce larger pixel values`() {
    val small = PdfRenderer.pointsToRenderPixels(10f)
    val medium = PdfRenderer.pointsToRenderPixels(20f)
    val large = PdfRenderer.pointsToRenderPixels(100f)

    assertTrue(small <= medium)
    assertTrue(medium <= large)
  }

  @Test
  fun `pointsToRenderPixels converts common values correctly`() {
    assertEquals(4, PdfRenderer.pointsToRenderPixels(1f))
    assertEquals(33, PdfRenderer.pointsToRenderPixels(8f))
    assertEquals(46, PdfRenderer.pointsToRenderPixels(11f))
    assertEquals(300, PdfRenderer.pointsToRenderPixels(72f))
  }
}
