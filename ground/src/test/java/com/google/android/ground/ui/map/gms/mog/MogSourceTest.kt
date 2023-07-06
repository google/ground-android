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

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MogSourceTest {
  private val mogSource = MogSource("url/{x}/{y}.tif", 5..7)

  @Test
  fun getMogUrl_whenZoomIsLessThanMinZoom_throwsError() {
    assertThrows(IllegalStateException::class.java) {
      mogSource.getMogUrl(TileCoordinates(250, 500, 2))
    }
  }

  @Test
  fun getMogUrl_whenZoomIsEqualToMinZoom_returnsMogUrlWithMinZoom() {
    assertThat(mogSource.getMogUrl(TileCoordinates(250, 500, 5))).isEqualTo("url/250/500.tif")
  }

  @Test
  fun getMogUrl_whenZoomIsGreaterThanMaxZoom_throwsError() {
    assertThrows(IllegalStateException::class.java) {
      mogSource.getMogUrl(TileCoordinates(2500, 5000, 9))
    }
  }

  @Test
  fun getMogBoundsForTile_whenZoomIsLessThanMinZoom_throwsError() {
    assertThrows(IllegalStateException::class.java) {
      mogSource.getMogBoundsForTile(TileCoordinates(10, 20, 4))
    }
  }

  @Test
  fun getMogBoundsForTile_whenZoomIsEqualToMinZoom_returnsSameCoordinate() {
    val testCoords = TileCoordinates(10, 20, 5)
    assertThat(mogSource.getMogBoundsForTile(testCoords)).isEqualTo(testCoords)
  }

  @Test
  fun getMogBoundsForTile_whenZoomIsMoreThanMinZoom_returnsScaledCoordinate() {
    val testCoords = TileCoordinates(10, 20, 6)
    assertThat(mogSource.getMogBoundsForTile(TileCoordinates(10, 20, 6)))
      .isEqualTo(TileCoordinates(5, 10, 5))
  }
}
