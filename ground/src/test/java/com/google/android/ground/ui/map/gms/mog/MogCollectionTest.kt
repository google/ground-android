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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MogCollectionTest {

  private lateinit var mogCollection: MogCollection

  @Before
  fun setUp() {
    mogCollection =
      MogCollection(
        TEST_WORLD_URL,
        TEST_HIGH_RES_URL,
        TEST_HIGH_RES_MIN_ZOOM,
        TEST_HIGH_RES_MAX_ZOOM
      )
  }

  @Test
  fun getMogUrl_whenZoomIsZero_returnsWorldMogUrlWithMinZoom() {
    assertThat(mogCollection.getMogUrl(TileCoordinates(10, 20, 0)))
      .isEqualTo("world_url/5/world.tif")
  }

  @Test
  fun getMogUrl_whenZoomIsLessThanMinZoom_throwsError() {
    assertThrows(IllegalStateException::class.java) {
      mogCollection.getMogUrl(TileCoordinates(10, 20, TEST_HIGH_RES_MIN_ZOOM - 1))
    }
  }

  @Test
  fun getMogUrl_whenZoomIsEqualToMinZoom_returnsHiResMogUrlWithMinZoom() {
    assertThat(mogCollection.getMogUrl(TileCoordinates(10, 20, TEST_HIGH_RES_MIN_ZOOM)))
      .isEqualTo("high_res_url/5/10/20.tif")
  }

  @Test
  fun getMogUrl_whenZoomIsMoreThanToMinZoom_returnsHiResMogUrlWithMinZoom() {
    assertThat(mogCollection.getMogUrl(TileCoordinates(10, 20, TEST_HIGH_RES_MIN_ZOOM + 1)))
      .isEqualTo("high_res_url/5/10/20.tif")
  }

  @Test
  fun getMogBoundsForTile_whenZoomIsLessThanMinZoom_returnsDefault() {
    assertThat(
        mogCollection.getMogBoundsForTile(TileCoordinates(10, 20, TEST_HIGH_RES_MIN_ZOOM - 1))
      )
      .isEqualTo(TileCoordinates.WORLD)
  }

  @Test
  fun getMogBoundsForTile_whenZoomIsEqualToMinZoom_returnsSameCoordinate() {
    assertThat(mogCollection.getMogBoundsForTile(TileCoordinates(10, 20, TEST_HIGH_RES_MIN_ZOOM)))
      .isEqualTo(TileCoordinates(10, 20, 5))
  }

  @Test
  fun getMogBoundsForTile_whenZoomIsMoreThanMinZoom_returnsScaledCoordinate() {
    assertThat(
        mogCollection.getMogBoundsForTile(TileCoordinates(10, 20, TEST_HIGH_RES_MIN_ZOOM + 1))
      )
      .isEqualTo(TileCoordinates(5, 10, 5))
  }

  companion object {
    const val TEST_WORLD_URL = "world_url/{z}/world.tif"
    const val TEST_HIGH_RES_URL = "high_res_url/{z}/{x}/{y}.tif"
    const val TEST_HIGH_RES_MIN_ZOOM = 5
    const val TEST_HIGH_RES_MAX_ZOOM = 14
  }
}
