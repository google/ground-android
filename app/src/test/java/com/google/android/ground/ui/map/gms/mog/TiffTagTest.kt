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
package com.google.android.ground.ui.map.gms.mog

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class TiffTagTest {

  @Test
  fun testUniqueId() {
    assertWithMessage("TiffTag enum contains non-unique ids")
      .that(TiffTag.entries.toTypedArray())
      .hasLength(TiffTag.byId.size)
  }

  @Test
  fun testIsArray_true() {
    TiffTag.entries
      .filter {
        it == TiffTag.TileByteCounts || it == TiffTag.TileOffsets || it == TiffTag.JPEGTables
      }
      .forEach { assertWithMessage("${it.name} should be of type array").that(it.isArray).isTrue() }
  }

  @Test
  fun testIsArray_false() {
    TiffTag.entries
      .filter {
        it != TiffTag.TileByteCounts && it != TiffTag.TileOffsets && it != TiffTag.JPEGTables
      }
      .forEach {
        assertWithMessage("${it.name} should not be of type array").that(it.isArray).isFalse()
      }
  }
}
