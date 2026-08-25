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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MogImageMetadataTest {

  // TileCountX = 4, TileCountY = 4
  private val testMogImageMetadata =
    MogImageMetadata(
      originTile = TileCoordinates(10, 10, 10),
      tileWidth = 256,
      tileLength = 256,
      tileOffsets = listOf(),
      byteCounts = listOf(),
      imageWidth = 1024,
      imageLength = 1024,
      jpegTables = ByteArray(10),
    )

  @Test
  fun `hasTile() returns true when coords in range`() {
    assertTrue(testMogImageMetadata.hasTile(12, 12))
  }

  @Test
  fun `hasTile() returns false when X coord out of range`() {
    assertFalse(testMogImageMetadata.hasTile(16, 12))
  }

  @Test
  fun `hasTile() returns false when Y coord out of range`() {
    assertFalse(testMogImageMetadata.hasTile(12, 16))
  }

  @Test
  fun `getByteRange() returns null when coords out of range`() {
    assertNull(testMogImageMetadata.getByteRange(12, 16))
  }

  @Test
  fun `getByteRange() throws error when index out of bounds`() {
    assertFailsWith<IllegalArgumentException> {
      testMogImageMetadata
        .copy(tileOffsets = LongRange(1, 12).toList(), byteCounts = LongRange(1, 12).toList())
        .getByteRange(13, 13)
    }
  }

  @Test
  fun `getByteRange() returns correct range`() {
    assertEquals(
      LongRange(6, 11),
      testMogImageMetadata
        .copy(tileOffsets = LongRange(1, 12).toList(), byteCounts = LongRange(1, 12).toList())
        .getByteRange(11, 11),
    )
  }

  @Test
  fun `equals() throws error`() {
    assertFailsWith<UnsupportedOperationException> {
      @Suppress("UnusedEquals")
      testMogImageMetadata.equals(testMogImageMetadata.copy(tileWidth = 100))
    }
  }

  @Test
  fun `hashCode() throws error`() {
    assertFailsWith<UnsupportedOperationException> { testMogImageMetadata.hashCode() }
  }
}
