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
package org.groundplatform.ui.util

import ground_android.core.ui.generated.resources.Res
import ground_android.core.ui.generated.resources.pdf_altitude
import ground_android.core.ui.generated.resources.skipped
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComposeStringResolverTest {

  @Test
  fun `resolves a plain string`() = runTest {
    assertEquals("Skipped", ComposeStringResolver.resolve(Res.string.skipped))
  }

  @Test
  fun `resolves a string with a format argument`() = runTest {
    assertEquals("Altitude: 5m", ComposeStringResolver.resolve(Res.string.pdf_altitude, "5"))
  }
}
