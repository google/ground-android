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
import kotlin.test.assertTrue

class MogTilesRequestTest {

  @Test
  fun `totalBytes returns sum of byte range counts`() {
    val tile1 = newTileMetadata(0..9) // 10 bytes
    val tile2 = newTileMetadata(10..29) // 20 bytes
    val request = MogTilesRequest("http://url", listOf(tile1, tile2))

    assertEquals(30, request.totalBytes)
  }

  @Test
  fun `byteRange returns range spanning from first to last tile`() {
    val tile1 = newTileMetadata(100..199)
    val tile2 = newTileMetadata(200..299)
    val request = MogTilesRequest("http://url", listOf(tile1, tile2))

    assertEquals(100L..299L, request.byteRange)
  }

  @Test
  fun `MutableMogTilesRequest appendTile succeeds when range is consecutive or later`() {
    val tile1 = newTileMetadata(0..9)
    val tile2 = newTileMetadata(10..19)
    val request = MutableMogTilesRequest("http://url", mutableListOf(tile1))

    request.appendTile(tile2)
    assertEquals(listOf(tile1, tile2), request.tiles)
  }

  @Test
  fun `MutableMogTilesRequest appendTile throws error when range is non-consecutive backwards or overlapping`() {
    val tile1 = newTileMetadata(10..20)
    val overlappingTile = newTileMetadata(15..25)
    val earlierTile = newTileMetadata(0..5)
    val request = MutableMogTilesRequest("http://url", mutableListOf(tile1))

    assertFailsWith<IllegalArgumentException> { request.appendTile(overlappingTile) }
    assertFailsWith<IllegalArgumentException> { request.appendTile(earlierTile) }
  }

  @Test
  fun `MutableMogTilesRequest canMergeWith returns expected results`() {
    val tile1 = newTileMetadata(0..10)
    val tile2 = newTileMetadata(15..25) // gap of 15 - 10 - 1 = 4 bytes
    val request1 = MutableMogTilesRequest("http://url1", mutableListOf(tile1))
    val request2 = MogTilesRequest("http://url1", listOf(tile2))
    val diffUrlRequest = MogTilesRequest("http://url2", listOf(tile2))

    assertFalse(request1.canMergeWith(diffUrlRequest, 10))
    assertFalse(request1.canMergeWith(request2, 3))
    assertTrue(request1.canMergeWith(request2, 4))
    assertTrue(request1.canMergeWith(request2, 10))
  }

  @Test
  fun `consolidate does not merge different URLs with consecutive ranges`() {
    val tile1 = newTileMetadata(0..10)
    val tile2 = newTileMetadata(11..20)
    val tile3 = newTileMetadata(21..30)
    val request1 = MogTilesRequest("http://url1", listOf(tile1))
    val request2 = MogTilesRequest("http://url2", listOf(tile2))
    val request3 = MogTilesRequest("http://url3", listOf(tile3))

    assertEquals(
      listOf(request1, request2, request3),
      listOf(request1, request2, request3).consolidate(0),
    )
  }

  @Test
  fun `consolidate does not merge same URLs with non-consecutive ranges`() {
    val tile1 = newTileMetadata(0..10)
    val tile2 = newTileMetadata(21..30)
    val request1 = MogTilesRequest("http://url", listOf(tile1))
    val request2 = MogTilesRequest("http://url", listOf(tile2))

    assertEquals(listOf(request1, request2), listOf(request1, request2).consolidate(0))
  }

  @Test
  fun `consolidate merges requests with same URLs and consecutive ranges`() {
    val tile1 = newTileMetadata(0..10)
    val tile2 = newTileMetadata(11..20)
    val tile3 = newTileMetadata(21..30)
    val request1 = MogTilesRequest("http://url", listOf(tile1))
    val request2 = MogTilesRequest("http://url", listOf(tile2))
    val request3 = MogTilesRequest("http://url", listOf(tile3))

    assertEquals(
      listOf(MogTilesRequest("http://url", listOf(tile1, tile2, tile3))),
      listOf(request1, request2, request3).consolidate(0),
    )
  }

  @Test
  fun `consolidate merges requests with same URLs and nearby ranges`() {
    val tile1 = newTileMetadata(0..10)
    val tile2 = newTileMetadata(12..20)
    val request1 = MogTilesRequest("http://url", listOf(tile1))
    val request2 = MogTilesRequest("http://url", listOf(tile2))

    assertEquals(
      listOf(MogTilesRequest("http://url", listOf(tile1, tile2))),
      listOf(request1, request2).consolidate(2),
    )
  }

  private fun newTileMetadata(byteRange: IntRange): MogTileMetadata =
    MogTileMetadata(
      TileCoordinates(0, 0, 0),
      256,
      256,
      byteArrayOf(),
      LongRange(byteRange.first.toLong(), byteRange.last.toLong()),
    )
}
