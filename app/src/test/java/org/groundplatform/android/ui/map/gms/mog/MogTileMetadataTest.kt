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
package org.groundplatform.android.ui.map.gms.mog

import org.junit.Assert.assertThrows
import org.junit.Test

class MogTileMetadataTest {

  private val testMogTileMetadata =
    MogTileMetadata(TileCoordinates(10, 20, 10), 100, 100, ByteArray(10), LongRange(0, 10))

  @Test
  fun testEquals_throwsError() {
    assertThrows(UnsupportedOperationException::class.java) {
      testMogTileMetadata.equals(testMogTileMetadata.copy(width = 100))
    }
  }

  @Test
  fun testHashcode_throwsError() {
    assertThrows(UnsupportedOperationException::class.java) { testMogTileMetadata.hashCode() }
  }
}
