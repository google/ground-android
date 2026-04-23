/*
 * Copyright 2023 Google LLC
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
package org.groundplatform.domain.model.imagery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TiffTagTest {

  @Test
  fun `unique id`() {
    assertEquals(
      TiffTag.byId.size,
      TiffTag.entries.toTypedArray().size,
      "TiffTag enum contains non-unique ids",
    )
  }

  @Test
  fun `is array when true`() {
    TiffTag.entries
      .filter {
        it == TiffTag.TileByteCounts || it == TiffTag.TileOffsets || it == TiffTag.JPEGTables
      }
      .forEach { assertTrue(it.isArray, "${it.name} should be of type array") }
  }

  @Test
  fun `is array when false`() {
    TiffTag.entries
      .filter {
        it != TiffTag.TileByteCounts && it != TiffTag.TileOffsets && it != TiffTag.JPEGTables
      }
      .forEach { assertFalse(it.isArray, "${it.name} should not be of type array") }
  }
}
