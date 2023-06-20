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

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
      jpegTables = ByteArray(10)
    )

  @Test
  fun testHasTile_whenInRange() {
    assertThat(testMogImageMetadata.hasTile(12, 12)).isTrue()
  }

  @Test
  fun testHasTile_whenXIsOutOfRange() {
    assertThat(testMogImageMetadata.hasTile(16, 12)).isFalse()
  }

  @Test
  fun testHasTile_whenYIsOutOfRange() {
    assertThat(testMogImageMetadata.hasTile(12, 16)).isFalse()
  }

  @Test
  fun testGetByteRange_whenNotInRange_isNull() {
    assertThat(testMogImageMetadata.getByteRange(12, 16)).isNull()
  }

  @Test
  fun testGetByteRange_whenInRangeButIndexMoreThanOffsets_throwsError() {
    assertThrows(IllegalArgumentException::class.java) {
      testMogImageMetadata
        .copy(tileOffsets = LongRange(1, 12).toList(), byteCounts = LongRange(1, 12).toList())
        .getByteRange(13, 13)
    }
  }

  @Test
  fun testGetByteRange_success() {
    assertThat(
        testMogImageMetadata
          .copy(tileOffsets = LongRange(1, 12).toList(), byteCounts = LongRange(1, 12).toList())
          .getByteRange(11, 11)
      )
      .isEqualTo(LongRange(6, 11))
  }

  @Test
  fun testEquals_throwsError() {
    assertThrows(UnsupportedOperationException::class.java) {
      testMogImageMetadata.equals(testMogImageMetadata.copy(tileWidth = 100))
    }
  }

  @Test
  fun testHashcode_throwsError() {
    assertThrows(UnsupportedOperationException::class.java) { testMogImageMetadata.hashCode() }
  }
}
