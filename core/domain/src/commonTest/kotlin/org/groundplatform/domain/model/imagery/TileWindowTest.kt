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

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.map.Bounds

class TileWindowTest {

  @Test
  fun `fitTo stops at the requested max zoom`() {
    // A tiny target would fit at any zoom, so the cap is what decides.
    val frame = TileWindow.fitTo(tinyBounds, sizePx = 512, maxZoom = 14)

    assertEquals(14, frame.zoom)
  }

  @Test
  fun `fitTo picks a shallower zoom for a target that would overflow the window`() {
    val tight = TileWindow.fitTo(largeBounds, sizePx = 512, maxZoom = 20)
    val loose = TileWindow.fitTo(tinyBounds, sizePx = 512, maxZoom = 20)

    assertTrue(tight.zoom < loose.zoom)
  }

  @Test
  fun `fitTo zooms out further for a smaller window`() {
    val small = TileWindow.fitTo(largeBounds, sizePx = 256, maxZoom = 20)
    val large = TileWindow.fitTo(largeBounds, sizePx = 1024, maxZoom = 20)

    assertTrue(small.zoom < large.zoom)
  }

  @Test
  fun `fitTo reserves padding on every edge`() {
    val unpadded = TileWindow.fitTo(largeBounds, sizePx = 512, maxZoom = 20, paddingPx = 0)
    val padded = TileWindow.fitTo(largeBounds, sizePx = 512, maxZoom = 20, paddingPx = 200)

    // Less usable room means the target only fits at a shallower zoom.
    assertTrue(padded.zoom < unpadded.zoom)
  }

  @Test
  fun `fitTo falls back to zoom 0 when the target fits at no zoom`() {
    val whole = Bounds(Coordinates(-80.0, -179.0), Coordinates(80.0, 179.0))

    val frame = TileWindow.fitTo(whole, sizePx = 64, maxZoom = 14)

    assertEquals(0, frame.zoom)
  }

  @Test
  fun `fitTo centers the window on the target`() {
    val target = largeBounds
    val frame = TileWindow.fitTo(target, sizePx = 512, maxZoom = 20)

    val center = Coordinates((target.north + target.south) / 2, (target.east + target.west) / 2)
    val projected = frame.offsetOf(center)
    assertTrue(abs(projected.x - 256) <= 2)
    assertTrue(abs(projected.y - 256) <= 2)
  }

  @Test
  fun `tilesAt covers the whole window`() {
    val frame = TileWindow(zoom = 14, sizePx = 512, originX = 100, originY = 100)

    val placements = frame.tilesAt(frame.zoom)

    // A 512px window starting at x=100 overlaps 3 × 3 tiles.
    assertEquals(9, placements.size)
    assertTrue(placements.all { it.tile.zoom == 14 })
    // Every placement lands inside the window.
    placements.forEach {
      val (x, y) = it.offset
      assertTrue(x + it.sizePx > 0 && x < frame.sizePx)
      assertTrue(y + it.sizePx > 0 && y < frame.sizePx)
    }
  }

  @Test
  fun `tilesAt is a single tile when the window sits inside one`() {
    val frame = TileWindow(zoom = 10, sizePx = 100, originX = 2560, originY = 2560)

    assertEquals(listOf(TileCoordinates(10, 10, 10)), frame.tilesAt(frame.zoom).map { it.tile })
  }

  @Test
  fun `tilesAt places a tile at the window origin when they share a corner`() {
    val frame = TileWindow(zoom = 12, sizePx = 256, originX = 512, originY = 768)

    val placement = frame.tilesAt(12).single()

    assertEquals(TileCoordinates(2, 3, 12), placement.tile)
    assertEquals(TileWindow.PixelOffset(0, 0), placement.offset)
  }

  @Test
  fun `tilesAt offsets a tile clipped by the window's edge negatively`() {
    val frame = TileWindow(zoom = 12, sizePx = 256, originX = 600, originY = 800)

    val placement = frame.tilesAt(12).first()

    assertEquals(TileCoordinates(2, 3, 12), placement.tile)
    assertEquals(TileWindow.PixelOffset(-88, -32), placement.offset)
  }

  @Test
  fun `tilesAt returns one tile when imagery zoom is lower than frame zoom`() {
    val frame = TileWindow(zoom = 20, sizePx = 512, originX = 49_152, originY = 49_152)

    val placement = frame.tilesAt(14).single()

    assertEquals(TileCoordinates(3, 3, 14), placement.tile)
    assertEquals(16_384, placement.sizePx)
    assertEquals(TileWindow.PixelOffset(0, 0), placement.offset)
  }

  @Test
  fun `tilesAt returns all lower zoom tiles overlapping the window`() {
    val frame = TileWindow(zoom = 20, sizePx = 512, originX = 49_052, originY = 49_152)

    val placements = frame.tilesAt(14)

    assertEquals(
      listOf(TileCoordinates(2, 3, 14), TileCoordinates(3, 3, 14)),
      placements.map { it.tile },
    )
    assertEquals(TileWindow.PixelOffset(-16_284, 0), placements.first().offset)
    assertEquals(TileWindow.PixelOffset(100, 0), placements.last().offset)
  }

  @Test
  fun `tilesAt uses native tile size at the frame zoom`() {
    val frame = TileWindow(zoom = 14, sizePx = 512, originX = 0, originY = 0)

    assertEquals(256, frame.tilesAt(14).first().sizePx)
  }

  @Test
  fun `tilesAt rejects imagery at a higher zoom than the frame`() {
    val frame = TileWindow(zoom = 14, sizePx = 512, originX = 0, originY = 0)

    assertFailsWith<IllegalArgumentException> { frame.tilesAt(15) }
  }

  @Test
  fun `offsetOf maps frame bounds to frame corners`() {
    val frame = TileWindow.fitTo(largeBounds, sizePx = 512, maxZoom = 20)

    val northwest = frame.offsetOf(frame.bounds.northwest)
    val southeast = frame.offsetOf(frame.bounds.southeast)

    // The frame bounds correspond exactly to the drawn window.
    assertTrue(abs(northwest.x) <= 1 && abs(northwest.y) <= 1)
    assertTrue(abs(southeast.x - 512) <= 1)
    assertTrue(abs(southeast.y - 512) <= 1)
  }

  @Test
  fun `bounds widen as the frame zooms out`() {
    val deep = TileWindow(zoom = 14, sizePx = 512, originX = 100_000, originY = 100_000)
    val shallow = TileWindow(zoom = 10, sizePx = 512, originX = 6_250, originY = 6_250)

    // At the same pixel size, a lower zoom covers a larger geographic area.
    assertTrue(shallow.bounds.east - shallow.bounds.west > deep.bounds.east - deep.bounds.west)
  }

  private companion object {
    val tinyBounds = Bounds(Coordinates(41.87600, 12.47570), Coordinates(41.87620, 12.47600))
    val largeBounds = Bounds(Coordinates(41.85, 12.42), Coordinates(41.92, 12.53))
  }
}
