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
package com.google.android.ground.ui.map.gms.mog

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TileCoordinatesTest {

  @Test
  fun fromLatLng() {
    assertThat(TileCoordinates.fromLatLng(LatLng(10.5, 20.5), 10))
      .isEqualTo(TileCoordinates(570, 481, 10))
  }

  @Test
  fun toStringCustomValue() {
    assertThat(TileCoordinates(10, 20, 10).toString()).isEqualTo("(10, 20) at zoom 10")
  }

  @Test
  fun withinBounds() {
    val tiles =
      TileCoordinates.withinBounds(LatLngBounds(LatLng(10.0, 10.0), LatLng(11.0, 11.0)), 10)

    assertThat(tiles).hasSize(16)
    assertThat(tiles)
      .containsExactly(
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
      )
  }

  @Test
  fun `getLatLngAtPixelOffset() returns correct coords`() {
    // TODO: Test these cases.
    //    println(TileCoordinates(0, 0, 0).getLatLngAtPixelOffset(0,0))
    //    println(TileCoordinates(0, 0, 0).getLatLngAtPixelOffset(128,128))
    //    println(TileCoordinates(0, 0, 0).getLatLngAtPixelOffset(255,255))
    //    println(TileCoordinates(0, 0, 1).getLatLngAtPixelOffset(0,0))
    //    println(TileCoordinates(0, 0, 1).getLatLngAtPixelOffset(511,511))
    //    println(TileCoordinates(1, 1, 1).getLatLngAtPixelOffset(0,0))
    //    println(TileCoordinates(1, 1, 1).getLatLngAtPixelOffset(511,511))
    // https://www.maptiler.com/google-maps-coordinates-tile-bounds-projection/#6/-37.07/-6.97
    //    println(TileCoordinates(25, 33, 6).getLatLngAtPixelOffset(0, 0))
    println(TileCoordinates(25, 33, 6).tileCoordsAndOffsetToLatLon(0, 0))
    //    println(TileCoordinates(25, 33, 6).toCoords(0, 0))

  }
}
