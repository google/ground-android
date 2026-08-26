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

package org.groundplatform.domain.model.imagery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MogMetadataTest {

  private val imageMetadataZoom5 =
    MogImageMetadata(
      originTile = TileCoordinates(10, 10, 5),
      tileWidth = 256,
      tileLength = 256,
      tileOffsets = listOf(0L),
      byteCounts = listOf(1024L),
      imageWidth = 256,
      imageLength = 256,
      jpegTables = byteArrayOf(),
    )

  private val imageMetadataZoom6 =
    MogImageMetadata(
      originTile = TileCoordinates(20, 20, 6),
      tileWidth = 256,
      tileLength = 256,
      tileOffsets = listOf(1024L),
      byteCounts = listOf(2048L),
      imageWidth = 512,
      imageLength = 512,
      jpegTables = byteArrayOf(),
    )

  private val mogMetadata =
    MogMetadata(
      sourceUrl = "https://example.com/world.tif",
      bounds = TileCoordinates(0, 0, 0),
      imageMetadata = listOf(imageMetadataZoom5, imageMetadataZoom6),
    )

  @Test
  fun `getImageMetadata returns correct metadata for existing zoom`() {
    assertEquals(imageMetadataZoom5, mogMetadata.getImageMetadata(5))
    assertEquals(imageMetadataZoom6, mogMetadata.getImageMetadata(6))
  }

  @Test
  fun `getImageMetadata returns null for non-existing zoom`() {
    assertNull(mogMetadata.getImageMetadata(4))
    assertNull(mogMetadata.getImageMetadata(7))
  }
}
