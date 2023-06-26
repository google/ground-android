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

package com.google.android.ground.model.geometry

import com.google.android.ground.model.geometry.GeometryValidator.isClosed
import com.google.android.ground.model.geometry.GeometryValidator.isComplete
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class GeometryValidatorTest {

  @Test
  fun validateLinearRing_empty_doesNothing() {
    LinearRing(listOf()).validate()
  }

  @Test
  fun validateLinearRing_firstAndLastNotSame_throws() {
    assertThrows("Invalid linear ring", InvalidGeometryException::class.java) {
      LinearRing(OPEN_LOOP).validate()
    }
  }

  @Test
  fun validateLinearRing_firstAndLastSame_doesNothing() {
    LinearRing(CLOSED_LOOP).validate()
  }

  @Test
  fun isClosedGeometry() {
    assertThat(Point(COORDINATE_1).isClosed()).isFalse()
    assertThat(LineString(OPEN_LOOP).isClosed()).isFalse()
    assertThat(LinearRing(CLOSED_LOOP).isClosed()).isTrue()
    assertThat(Polygon(LinearRing(CLOSED_LOOP)).isClosed()).isTrue()
    assertThat(MultiPolygon(listOf(Polygon(LinearRing(CLOSED_LOOP)))).isClosed()).isFalse()
  }

  @Test
  fun isComplete_whenCountIsLessThanFour() {
    assertThat(OPEN_LOOP.isComplete()).isFalse()
  }

  @Test
  fun isComplete_whenCountIsFour_whenFirstAndLastAreNotSame() {
    assertThat((OPEN_LOOP + COORDINATE_4).isComplete()).isFalse()
  }

  @Test
  fun isComplete_whenCountIsFour_whenFirstAndLastAreSame() {
    assertThat(CLOSED_LOOP.isComplete()).isTrue()
  }

  companion object {
    private val COORDINATE_1 = Coordinate(10.0, 10.0)
    private val COORDINATE_2 = Coordinate(20.0, 20.0)
    private val COORDINATE_3 = Coordinate(30.0, 30.0)
    private val COORDINATE_4 = Coordinate(40.0, 40.0)

    private val OPEN_LOOP = listOf(COORDINATE_1, COORDINATE_2, COORDINATE_3)
    private val CLOSED_LOOP = listOf(COORDINATE_1, COORDINATE_2, COORDINATE_3, COORDINATE_1)
  }
}
