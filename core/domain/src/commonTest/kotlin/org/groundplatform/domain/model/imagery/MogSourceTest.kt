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

class MogSourceTest {
  private val mogSource = MogSource(5..7, "url/{x}/{y}.tif")

  @Test
  fun `getMogPath throws an error when zoom is less than min zoom`() {
    assertFailsWith<IllegalStateException> { mogSource.getMogPath(TileCoordinates(250, 500, 2)) }
  }

  @Test
  fun `getMogPath returns path when zoom is equal to min zoom`() {
    assertEquals("url/250/500.tif", mogSource.getMogPath(TileCoordinates(250, 500, 5)))
  }

  @Test
  fun `getMogPath throws an error when zoom is greater than max zoom`() {
    assertFailsWith<IllegalStateException> { mogSource.getMogPath(TileCoordinates(2500, 5000, 9)) }
  }

  @Test
  fun `getMogBoundsForTile throws an error when zoom is LessThanMinZoom`() {
    assertFailsWith<IllegalStateException> {
      mogSource.getMogBoundsForTile(TileCoordinates(10, 20, 4))
    }
  }

  @Test
  fun `getMogBoundsForTile returnsSameCoordinates when zoom is EqualToMinZoom`() {
    val testCoords = TileCoordinates(10, 20, 5)
    assertEquals(testCoords, mogSource.getMogBoundsForTile(testCoords))
  }

  @Test
  fun `getMogBoundsForTile returnsScaledCoordinates when zoom is MoreThanMinZoom`() {
    assertEquals(
      TileCoordinates(5, 10, 5),
      mogSource.getMogBoundsForTile(TileCoordinates(10, 20, 6)),
    )
  }

  @Test
  fun `list minZoom, maxZoom, and zoomRange return correct values`() {
    val sources = listOf(MogSource(0..4, "overview.tif"), MogSource(5..14, "{x}/{y}.tif"))
    assertEquals(0, sources.minZoom())
    assertEquals(14, sources.maxZoom())
    assertEquals(0..14, sources.zoomRange())
  }
}
