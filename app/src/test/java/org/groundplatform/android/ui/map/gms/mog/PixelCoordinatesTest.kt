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

import com.google.android.gms.maps.model.LatLng
import kotlin.test.assertEquals
import org.junit.Test

class PixelCoordinatesTest {
  @Test
  fun `atZoom() at lower zoom reduces coords`() {
    val actual = PixelCoordinates(128, 256, 6).atZoom(4)
    val expected = PixelCoordinates(32, 64, 4)
    assertEquals(expected, actual)
  }

  @Test
  fun `atZoom() at higher zoom increases coords`() {
    val actual = PixelCoordinates(256, 512, 0).atZoom(3)
    val expected = PixelCoordinates(2048, 4096, 3)
    assertEquals(expected, actual)
  }

  @Test
  fun `atZoom() at same zoom no-op`() {
    val actual = PixelCoordinates(128, 32, 5).atZoom(5)
    val expected = PixelCoordinates(128, 32, 5)
    assertEquals(expected, actual)
  }

  @Test
  fun `LatLng#toPixelCoordinates() at zoom 0 returns correct values`() {
    // jsfiddle used to find examples: https://jsfiddle.net/gmiceli/pjhy4Lfm/16/
    val actual = LatLng(40.6874, -73.9306).toPixelCoordinates(0)
    val expected = PixelCoordinates(75, 96, 0)
    assertEquals(expected, actual)
  }

  @Test
  fun `LatLng#toPixelCoordinates() at intermediate zoom returns correct values`() {
    // jsfiddle used to find examples: https://jsfiddle.net/gmiceli/pjhy4Lfm/16/
    val actual = LatLng(41.876, 12.4757).toPixelCoordinates(3)
    val expected = PixelCoordinates(1094, 761, 3)
    assertEquals(expected, actual)
  }

  @Test
  fun `TileCoordinates#toPixelCoordinate() at zoom 0 returns valid result`() {
    val expected = PixelCoordinates(128, 128, 0)
    val actual = TileCoordinates(0, 0, 0).toPixelCoordinate(128, 128)
    assertEquals(expected, actual)
  }

  @Test
  fun `TileCoordinates#toPixelCoordinate() at intermediate zoom returns valid result`() {
    val expected = PixelCoordinates(527, 537, 2)
    val actual = TileCoordinates(2, 2, 2).toPixelCoordinate(15, 25)
    assertEquals(expected, actual)
  }
}
