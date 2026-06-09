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
package org.groundplatform.feature.pdf

import kotlin.test.assertEquals
import org.junit.Test

class AndroidPdfImageProviderTest {

  @Test
  fun `calculateInSampleSize returns 1 when the image is smaller than the target`() {
    assertEquals(1, calculateInSampleSize(width = 100, height = 100, target = 200))
  }

  @Test
  fun `calculateInSampleSize returns 1 when the image equals the target`() {
    assertEquals(1, calculateInSampleSize(width = 200, height = 200, target = 200))
  }

  @Test
  fun `calculateInSampleSize halves once when both dimensions are at least double the target`() {
    // 400/2 = 200 >= 200, but 400/4 = 100 < 200, so the largest valid power of two is 2.
    assertEquals(2, calculateInSampleSize(width = 400, height = 400, target = 200))
  }

  @Test
  fun `calculateInSampleSize returns the largest power of two that keeps both axes above target`() {
    // 800/4 = 200 >= 200, 800/8 = 100 < 200, so the result is 4.
    assertEquals(4, calculateInSampleSize(width = 800, height = 800, target = 200))
  }

  @Test
  fun `calculateInSampleSize is limited by the smaller dimension`() {
    // Width could be sampled further, but height (300) only tolerates a sample size of 1
    // because 300/2 = 150 < 200.
    assertEquals(1, calculateInSampleSize(width = 1600, height = 300, target = 200))
  }
}
