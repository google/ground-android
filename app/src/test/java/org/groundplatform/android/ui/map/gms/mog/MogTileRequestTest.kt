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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MogTileRequestTest {

  @Test
  fun `consolidate does not merge different URLs with consecutive ranges`() {
    val tile1 = newTileMetadata(0..10)
    val tile2 = newTileMetadata(11..20)
    val tile3 = newTileMetadata(21..30)
    val request1 = MogTilesRequest("http://url1", listOf(tile1))
    val request2 = MogTilesRequest("http://url2", listOf(tile2))
    val request3 = MogTilesRequest("http://url3", listOf(tile3))

    assertThat(listOf(request1, request2, request3).consolidate(0))
      .containsExactly(request1, request2, request3)
  }

  @Test
  fun `consolidate does not merge same URLs with non-consecutive ranges`() {
    val tile1 = newTileMetadata(0..10)
    val tile2 = newTileMetadata(21..30)
    val request1 = MogTilesRequest("http://url", listOf(tile1))
    val request2 = MogTilesRequest("http://url", listOf(tile2))

    assertThat(listOf(request1, request2).consolidate(0)).containsExactly(request1, request2)
  }

  @Test
  fun `consolidate merges requests with same URLs and consecutive ranges`() {
    val tile1 = newTileMetadata(0..10)
    val tile2 = newTileMetadata(11..20)
    val tile3 = newTileMetadata(21..30)
    val request1 = MogTilesRequest("http://url", listOf(tile1))
    val request2 = MogTilesRequest("http://url", listOf(tile2))
    val request3 = MogTilesRequest("http://url", listOf(tile3))

    assertThat(listOf(request1, request2, request3).consolidate(0))
      .containsExactly(MogTilesRequest("http://url", listOf(tile1, tile2, tile3)))
  }

  @Test
  fun `consolidate merges requests with same URLs and nearby ranges`() {
    val tile1 = newTileMetadata(0..10)
    val tile2 = newTileMetadata(12..20)
    val request1 = MogTilesRequest("http://url", listOf(tile1))
    val request2 = MogTilesRequest("http://url", listOf(tile2))

    assertThat(listOf(request1, request2).consolidate(2))
      .containsExactly(MogTilesRequest("http://url", listOf(tile1, tile2)))
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
