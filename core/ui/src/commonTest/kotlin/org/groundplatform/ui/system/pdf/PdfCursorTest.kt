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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PdfCursorTest {

  @Test
  fun `fresh cursor starts at the top margin`() {
    val cursor = PdfCursor()

    assertEquals(PdfConfig.MARGIN.toFloat(), cursor.y)
    assertTrue(cursor.isAtPageTop)
  }

  @Test
  fun `advance moves the cursor down by the given delta`() {
    val cursor = PdfCursor()
    val start = cursor.y

    cursor.advance(75f)

    assertEquals(start + 75f, cursor.y)
    assertFalse(cursor.isAtPageTop)
  }

  @Test
  fun `moveTo sets the cursor to an absolute Y`() {
    val cursor = PdfCursor()

    cursor.moveTo(400f)

    assertEquals(400f, cursor.y)
  }

  @Test
  fun `reset returns the cursor to the top margin`() {
    val cursor = PdfCursor()
    cursor.advance(300f)

    cursor.reset()

    assertEquals(PdfConfig.MARGIN.toFloat(), cursor.y)
    assertTrue(cursor.isAtPageTop)
  }

  @Test
  fun `isAtPageTop reflects whether Y matches the top margin`() {
    val cursor = PdfCursor()
    assertTrue(cursor.isAtPageTop)

    cursor.advance(1f)
    assertFalse(cursor.isAtPageTop)

    cursor.moveTo(PdfConfig.MARGIN.toFloat())
    assertTrue(cursor.isAtPageTop)
  }

  @Test
  fun `fits returns true when there is room above the bottom margin`() {
    val cursor = PdfCursor()

    val available = PdfConfig.PAGE_HEIGHT - 2 * PdfConfig.MARGIN
    assertTrue(cursor.fits(available.toFloat()))
    assertTrue(cursor.fits(10f))
  }

  @Test
  fun `fits returns false when the requested height overflows the bottom margin`() {
    val cursor = PdfCursor()

    val available = PdfConfig.PAGE_HEIGHT - 2 * PdfConfig.MARGIN
    assertFalse(cursor.fits(available + 1f))
  }

  @Test
  fun `fits subtracts the footer reserve from the available space`() {
    val cursor = PdfCursor()
    val available = (PdfConfig.PAGE_HEIGHT - 2 * PdfConfig.MARGIN).toFloat()
    cursor.footerReserve = 50f

    assertTrue(cursor.fits(available - 50f))
    assertFalse(
      cursor.fits(available - 49f)
    )
  }

  @Test
  fun `fits depends on the current Y position`() {
    val cursor = PdfCursor()
    val available = (PdfConfig.PAGE_HEIGHT - 2 * PdfConfig.MARGIN).toFloat()

    cursor.advance(100f)

    assertTrue(cursor.fits(available - 100f))
    assertFalse(cursor.fits(available - 99f))
  }

  @Test
  fun `custom page height and margin are respected`() {
    val cursor = PdfCursor(pageHeight = 200, margin = 10)

    assertEquals(10f, cursor.y)
    // Usable height = 200 - 2*10 = 180.
    assertTrue(cursor.fits(180f))
    assertFalse(cursor.fits(181f))
  }
}