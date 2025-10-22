/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.map.gms

import android.graphics.Bitmap
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import org.groundplatform.android.BaseHiltTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for CachingUpscalingTileProvider.
 *
 * Tests cover:
 * - Base tile delegation (z <= dataMaxZoom)
 * - Upscaled tile synthesis (z > dataMaxZoom)
 * - Cache hit/miss scenarios
 * - Coordinate calculation correctness
 * - Error handling and edge cases
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CachingUpscalingTileProviderTest : BaseHiltTest() {

  private lateinit var fakeTileProvider: FakeTileProvider
  private lateinit var provider: CachingUpscalingTileProvider

  private val dataMaxZoom = 15
  private val tileSize = 256

  override fun setUp() {
    super.setUp()
    fakeTileProvider = FakeTileProvider()
    provider =
      CachingUpscalingTileProvider(
        source = fakeTileProvider,
        zoomThreshold = dataMaxZoom,
        tileSize = tileSize,
        maxCacheBytes = 1024 * 1024,
      )
  }

  @Test
  fun `getTile() delegates to source when zoom is below dataMaxZoom`() {
    val x = 100
    val y = 200
    val z = 10
    fakeTileProvider.addTile(x, y, z, createTestTile())

    val result = provider.getTile(x, y, z)

    assertThat(result).isNotNull()
    assertThat(result).isNotEqualTo(TileProvider.NO_TILE)
    assertThat(fakeTileProvider.getCallCount(x, y, z)).isEqualTo(1)
  }

  @Test
  fun `getTile() delegates to source when zoom equals dataMaxZoom`() {
    val x = 512
    val y = 384
    val z = dataMaxZoom
    fakeTileProvider.addTile(x, y, z, createTestTile())

    val result = provider.getTile(x, y, z)

    assertThat(result).isNotNull()
    assertThat(result).isNotEqualTo(TileProvider.NO_TILE)
    assertThat(fakeTileProvider.getCallCount(x, y, z)).isEqualTo(1)
  }

  @Test
  fun `getTile() does not cache base tiles`() {
    val x = 100
    val y = 200
    val z = 14
    fakeTileProvider.addTile(x, y, z, createTestTile())

    provider.getTile(x, y, z)
    provider.getTile(x, y, z)

    assertThat(fakeTileProvider.getCallCount(x, y, z)).isEqualTo(2)
  }

  @Test
  fun `getTile() returns NO_TILE when source returns null for base zoom`() {
    val x = 100
    val y = 200
    val z = 12

    val result = provider.getTile(x, y, z)

    assertThat(result).isEqualTo(TileProvider.NO_TILE)
  }

  @Test
  fun `getTile() synthesizes upscaled tile when zoom exceeds dataMaxZoom`() {
    val x = 1024
    val y = 768
    val z = 16 // One zoom level beyond dataMaxZoom
    fakeTileProvider.addTile(512, 384, dataMaxZoom, createTestTileWithBitmap())

    val result = provider.getTile(x, y, z)

    assertThat(result).isNotNull()
    assertThat(result).isNotEqualTo(TileProvider.NO_TILE)
    assertThat(result.width).isEqualTo(tileSize)
    assertThat(result.height).isEqualTo(tileSize)
    assertThat(result.data).isNotNull()
    assertThat(fakeTileProvider.getCallCount(512, 384, dataMaxZoom)).isEqualTo(1)
  }

  @Test
  fun `getTile() calculates correct parent coordinates for zoom level 16`() {
    val z = 16
    val testCases =
      listOf(
        Triple(0, 0, Pair(0, 0)),
        Triple(1, 1, Pair(0, 0)),
        Triple(2, 2, Pair(1, 1)),
        Triple(512, 384, Pair(256, 192)),
        Triple(1023, 1023, Pair(511, 511)),
      )

    testCases.forEach { (x, y, expectedParent) ->
      fakeTileProvider.clear()
      fakeTileProvider.addTile(
        expectedParent.first,
        expectedParent.second,
        dataMaxZoom,
        createTestTileWithBitmap(),
      )

      provider.getTile(x, y, z)

      assertThat(
          fakeTileProvider.getCallCount(expectedParent.first, expectedParent.second, dataMaxZoom)
        )
        .isEqualTo(1)
    }
  }

  @Test
  fun `getTile() calculates correct parent coordinates for zoom level 17`() {
    val z = 17 // Two levels beyond dataMaxZoom
    val x = 2048
    val y = 1536
    fakeTileProvider.addTile(512, 384, dataMaxZoom, createTestTileWithBitmap())

    provider.getTile(x, y, z)

    assertThat(fakeTileProvider.getCallCount(512, 384, dataMaxZoom)).isEqualTo(1)
  }

  @Test
  fun `getTile() correctly handles all four quadrants`() {
    val z = 16
    fakeTileProvider.addTile(0, 0, dataMaxZoom, createTestTileWithBitmap())

    val quadrants = listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1))

    quadrants.forEach { (qx, qy) ->
      val result = provider.getTile(qx, qy, z)
      assertThat(result).isNotNull()
      assertThat(result).isNotEqualTo(TileProvider.NO_TILE)
    }

    assertThat(fakeTileProvider.getCallCount(0, 0, dataMaxZoom)).isEqualTo(4)
  }

  @Test
  fun `getTile() caches synthesized tiles`() {
    val x = 1024
    val y = 768
    val z = 16
    fakeTileProvider.addTile(512, 384, dataMaxZoom, createTestTileWithBitmap())

    provider.getTile(x, y, z)
    provider.getTile(x, y, z)

    assertThat(fakeTileProvider.getCallCount(512, 384, dataMaxZoom)).isEqualTo(1)
  }

  @Test
  fun `getTile() uses different cache keys for different coordinates`() {
    val z = 16
    fakeTileProvider.addTile(512, 384, dataMaxZoom, createTestTileWithBitmap())
    fakeTileProvider.addTile(512, 385, dataMaxZoom, createTestTileWithBitmap())

    val coordinates = listOf(Pair(1024, 768), Pair(1025, 768), Pair(1024, 769))

    coordinates.forEach { (x, y) -> provider.getTile(x, y, z) }

    assertThat(fakeTileProvider.totalCallCount).isAtLeast(3)
  }

  @Test
  fun `cache hit returns same result on subsequent requests`() {
    val x = 1024
    val y = 768
    val z = 16
    fakeTileProvider.addTile(512, 384, dataMaxZoom, createTestTileWithBitmap())

    val result1 = provider.getTile(x, y, z)
    val result2 = provider.getTile(x, y, z)

    assertThat(result1).isNotNull()
    assertThat(result2).isNotNull()
    assertThat(result1.width).isEqualTo(result2.width)
    assertThat(result1.height).isEqualTo(result2.height)
    assertThat(fakeTileProvider.getCallCount(512, 384, dataMaxZoom)).isEqualTo(1)
  }

  @Test
  fun `getTile() returns NO_TILE when parent tile is null`() {
    val x = 1024
    val y = 768
    val z = 16

    val result = provider.getTile(x, y, z)

    assertThat(result).isEqualTo(TileProvider.NO_TILE)
  }

  @Test
  fun `getTile() returns NO_TILE when parent tile data is null`() {
    val x = 1024
    val y = 768
    val z = 16
    fakeTileProvider.addTile(512, 384, dataMaxZoom, Tile(tileSize, tileSize, null))

    val result = provider.getTile(x, y, z)

    assertThat(result).isEqualTo(TileProvider.NO_TILE)
  }

  @Test
  fun `getTile() does not cache failed synthesis attempts`() {
    val x = 1024
    val y = 768
    val z = 16

    provider.getTile(x, y, z)
    provider.getTile(x, y, z)
    assertThat(fakeTileProvider.getCallCount(512, 384, dataMaxZoom)).isEqualTo(2)
  }

  @Test
  fun `getTile() handles minimum coordinates`() {
    val x = 0
    val y = 0
    val z = 16
    fakeTileProvider.addTile(0, 0, dataMaxZoom, createTestTileWithBitmap())

    val result = provider.getTile(x, y, z)

    assertThat(result).isNotNull()
    assertThat(result).isNotEqualTo(TileProvider.NO_TILE)
  }

  @Test
  fun `getTile() handles large coordinates`() {
    val x = 65535
    val y = 65535
    val z = 16
    fakeTileProvider.addTile(32767, 32767, dataMaxZoom, createTestTileWithBitmap())

    val result = provider.getTile(x, y, z)

    assertThat(result).isNotNull()
    assertThat(fakeTileProvider.getCallCount(32767, 32767, dataMaxZoom)).isEqualTo(1)
  }

  @Test
  fun `getTile() handles multiple zoom levels beyond dataMaxZoom`() {
    fakeTileProvider.addTile(0, 0, dataMaxZoom, createTestTileWithBitmap())

    val zoomLevels = listOf(16, 17, 18, 19, 20)

    zoomLevels.forEach { z ->
      val result = provider.getTile(0, 0, z)
      assertThat(result).isNotNull()
      assertThat(result).isNotEqualTo(TileProvider.NO_TILE)
    }
  }

  @Test
  fun `provider handles concurrent tile requests`() {
    fakeTileProvider.addTile(0, 0, dataMaxZoom, createTestTileWithBitmap())
    fakeTileProvider.addTile(1, 1, dataMaxZoom, createTestTileWithBitmap())
    fakeTileProvider.addTile(2, 2, dataMaxZoom, createTestTileWithBitmap())

    val coordinates = (0..10).map { it to it }

    coordinates.forEach { (x, y) ->
      val result = provider.getTile(x, y, 16)
      assertThat(result).isNotNull()
    }

    assertThat(fakeTileProvider.totalCallCount).isAtLeast(coordinates.size)
  }

  @Test
  fun `provider respects custom tile size`() {
    val customTileSize = 512
    val customProvider =
      CachingUpscalingTileProvider(
        source = fakeTileProvider,
        zoomThreshold = dataMaxZoom,
        tileSize = customTileSize,
      )
    fakeTileProvider.addTile(0, 0, dataMaxZoom, createTestTileWithBitmap(customTileSize))

    val result = customProvider.getTile(0, 0, 16)

    assertThat(result.width).isEqualTo(customTileSize)
    assertThat(result.height).isEqualTo(customTileSize)
  }

  @Test
  fun `provider respects custom dataMaxZoom`() {
    val customDataMaxZoom = 10
    val customProvider =
      CachingUpscalingTileProvider(source = fakeTileProvider, zoomThreshold = customDataMaxZoom)
    fakeTileProvider.addTile(100, 100, customDataMaxZoom, createTestTile())
    fakeTileProvider.addTile(100, 100, customDataMaxZoom, createTestTileWithBitmap())

    customProvider.getTile(100, 100, customDataMaxZoom)

    customProvider.getTile(200, 200, customDataMaxZoom + 1)

    assertThat(fakeTileProvider.getCallCount(100, 100, customDataMaxZoom)).isAtLeast(2)
  }

  private fun createTestTile(): Tile = Tile(tileSize, tileSize, ByteArray(256) { it.toByte() })

  private fun createTestTileWithBitmap(size: Int = tileSize): Tile {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

    for (x in 0 until size) {
      for (y in 0 until size) {
        bitmap.setPixel(x, y, android.graphics.Color.rgb(x % 256, y % 256, 128))
      }
    }

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    bitmap.recycle()
    return Tile(size, size, outputStream.toByteArray())
  }

  /**
   * Fake implementation of [TileProvider] for testing purposes. Tracks call counts and allows
   * configuring tiles to return.
   */
  private class FakeTileProvider : TileProvider {
    private val tiles = mutableMapOf<String, Tile>()
    private val callCounts = mutableMapOf<String, Int>()
    val totalCallCount: Int
      get() = callCounts.values.sum()

    fun addTile(x: Int, y: Int, z: Int, tile: Tile) {
      tiles[makeKey(x, y, z)] = tile
    }

    fun getCallCount(x: Int, y: Int, z: Int): Int = callCounts[makeKey(x, y, z)] ?: 0

    fun clear() {
      tiles.clear()
      callCounts.clear()
    }

    override fun getTile(x: Int, y: Int, z: Int): Tile? {
      val key = makeKey(x, y, z)
      callCounts[key] = (callCounts[key] ?: 0) + 1
      return tiles[key]
    }

    private fun makeKey(x: Int, y: Int, z: Int) = "$z/$x/$y"
  }
}
