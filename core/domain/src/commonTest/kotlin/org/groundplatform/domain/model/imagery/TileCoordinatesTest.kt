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
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.map.Bounds

class TileCoordinatesTest {

  @Test
  fun `creates tile coordinates from lat lng correctly`() {
    assertEquals(
      TileCoordinates(570, 481, 10),
      TileCoordinates.fromCoordinates(Coordinates(10.5, 20.5), 10),
    )
  }

  @Test
  fun `toString returns custom formatted value`() {
    assertEquals("(10, 20) at zoom 10", TileCoordinates(10, 20, 10).toString())
  }

  @Test
  fun `returns tiles within bounds correctly`() {
    val tiles = TileCoordinates.withinBounds(Bounds(10.0, 10.0, 11.0, 11.0), 10)

    assertEquals(16, tiles.size)
    assertEquals(
      listOf(
        TileCoordinates(540, 480, 10),
        TileCoordinates(541, 480, 10),
        TileCoordinates(542, 480, 10),
        TileCoordinates(543, 480, 10),
        TileCoordinates(540, 481, 10),
        TileCoordinates(541, 481, 10),
        TileCoordinates(542, 481, 10),
        TileCoordinates(543, 481, 10),
        TileCoordinates(540, 482, 10),
        TileCoordinates(541, 482, 10),
        TileCoordinates(542, 482, 10),
        TileCoordinates(543, 482, 10),
        TileCoordinates(540, 483, 10),
        TileCoordinates(541, 483, 10),
        TileCoordinates(542, 483, 10),
        TileCoordinates(543, 483, 10),
      ),
      tiles,
    )
  }
}
