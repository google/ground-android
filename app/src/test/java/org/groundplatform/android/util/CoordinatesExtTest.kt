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

package org.groundplatform.android.util

import com.google.android.gms.maps.model.LatLng
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import org.groundplatform.android.model.geometry.Coordinates
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CoordinatesExtTest {

  @Test
  fun `process coordinates when ne`() {
    assertThat(Coordinates(10.555, 10.555).toDmsFormat()).isEqualTo("10°33'18\" N 10°33'18\" E")
  }

  @Test
  fun `process coordinates when se`() {
    assertThat(Coordinates(-10.555, 10.555).toDmsFormat()).isEqualTo("10°33'18\" S 10°33'18\" E")
  }

  @Test
  fun `process coordinates when nw`() {
    assertThat(Coordinates(10.555, -10.555).toDmsFormat()).isEqualTo("10°33'18\" N 10°33'18\" W")
  }

  @Test
  fun `process coordinates when sw`() {
    assertThat(Coordinates(-10.555, -10.555).toDmsFormat()).isEqualTo("10°33'18\" S 10°33'18\" W")
  }

  @Test
  fun `midpoint when simple case`() {
    val coord1 = Coordinates(lat = 0.0, lng = 0.0)
    val coord2 = Coordinates(lat = 2.0, lng = 2.0)
    val expected = LatLng(1.0, 1.0)

    val result = coord1.midpoint(coord2)

    assertEquals("Latitude should be averaged correctly", expected.latitude, result.latitude, 1e-5)
    assertEquals(
      "Longitude should be averaged correctly",
      expected.longitude,
      result.longitude,
      1e-5,
    )
  }

  @Test
  fun `midpoint when negative coordinates`() {
    val coord1 = Coordinates(lat = -1.0, lng = -1.0)
    val coord2 = Coordinates(lat = 1.0, lng = 1.0)
    val expected = LatLng(0.0, 0.0)

    val result = coord1.midpoint(coord2)

    assertEquals(
      "Latitude should be averaged correctly with negatives",
      expected.latitude,
      result.latitude,
      1e-5,
    )
    assertEquals(
      "Longitude should be averaged correctly with negatives",
      expected.longitude,
      result.longitude,
      1e-5,
    )
  }

  @Test
  fun `midpoint when mixed coordinates`() {
    // Given a case with mixed (positive and negative) values
    val coord1 = Coordinates(lat = 10.0, lng = -5.0)
    val coord2 = Coordinates(lat = 20.0, lng = 15.0)
    val expected = LatLng(15.0, 5.0)

    // When
    val result = coord1.midpoint(coord2)

    // Then
    assertEquals(
      "Latitude should be averaged correctly with mixed values",
      expected.latitude,
      result.latitude,
      1e-5,
    )
    assertEquals(
      "Longitude should be averaged correctly with mixed values",
      expected.longitude,
      result.longitude,
      1e-5,
    )
  }
}
