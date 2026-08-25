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
package org.groundplatform.ui.components.qrcode

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QrCodeGeneratorTest {

  @Test
  fun `fitsLogoCapacity is true for empty content`() {
    assertTrue(fitsLogoCapacity(""))
  }

  @Test
  fun `fitsLogoCapacity is true for content below the byte limit`() {
    assertTrue(fitsLogoCapacity("1".repeat(MAX_QR_BYTES_WITH_LOGO - 1)))
  }

  @Test
  fun `fitsLogoCapacity is true for content exactly at the byte limit`() {
    assertTrue(fitsLogoCapacity("1".repeat(MAX_QR_BYTES_WITH_LOGO)))
  }

  @Test
  fun `fitsLogoCapacity is false for content above the byte limit`() {
    assertFalse(fitsLogoCapacity("1".repeat(MAX_QR_BYTES_WITH_LOGO + 1)))
  }

  @Test
  fun `fitsLogoCapacity counts UTF-8 bytes rather than characters`() {
    // Each "€" encodes to 3 UTF-8 bytes, so this exceeds the limit despite the smaller char count.
    val charCount = MAX_QR_BYTES_WITH_LOGO / 3 + 1
    val content = "€".repeat(charCount)

    assertTrue(content.length <= MAX_QR_BYTES_WITH_LOGO)
    assertFalse(fitsLogoCapacity(content))
  }
}
