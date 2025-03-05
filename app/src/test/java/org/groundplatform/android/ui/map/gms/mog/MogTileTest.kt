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

class MogTileTest {

  @Test
  fun `toGmsTile() returns valid tile`() {
    val mogTile = MogTile(MOG_TILE_METADATA, byteArrayOf(0, 1, 2, 3, 4))
    val gmsTile = mogTile.toGmsTile()
    assertThat(gmsTile.width).isEqualTo(WIDTH)
    assertThat(gmsTile.height).isEqualTo(HEIGHT)
    assertThat(gmsTile.data).isEqualTo(EXPECTED_DATA)
  }

  companion object {
    private const val WIDTH = 100
    private const val HEIGHT = 200
    private val MOG_TILE_METADATA =
      MogTileMetadata(
        tileCoordinates = TileCoordinates(0, 0, 0),
        width = WIDTH,
        height = HEIGHT,
        jpegTables = byteArrayOf(100, 101, 102, 103, 104),
        byteRange = LongRange(0, 10),
      )
    private val EXPECTED_DATA =
      byteArrayOf(
        -1,
        -40,
        -1,
        -32,
        0,
        16,
        74,
        70,
        73,
        70,
        0,
        1,
        2,
        0,
        0,
        100,
        0,
        -56,
        0,
        0,
        102,
        2,
        3,
        4,
        -1,
        -39,
      )
  }
}
