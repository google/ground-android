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

import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.remote.firebase.schema.Path
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test

class GeometryTest {
  private val x = -42.121
  private val y = 28.482
  private val path1 =
    arrayOf(
      -89.63410225 to 41.89729784,
      -89.63805046 to 41.89525340,
      -89.63659134 to 41.88937530,
      -89.62886658 to 41.88956698,
      -89.62800827 to 41.89544507,
      -89.63410225 to 41.89729784,
    )
  private val path2 =
    arrayOf(
      -89.63453141 to 41.89193106,
      -89.63118400 to 41.89090878,
      -89.63066902 to 41.89397560,
      -89.63358726 to 41.89480618,
      -89.63453141 to 41.89193106,
    )
  private val path3 =
    arrayOf(
      -89.61006966 to 41.89333669,
      -89.61479034 to 41.89832003,
      -89.61719360 to 41.89455062,
      -89.61521950 to 41.89154771,
      -89.61006966 to 41.89333669,
    )
  private val path4 =
    arrayOf(
      -89.61393204 to 41.89320891,
      -89.61290207 to 41.89429505,
      -89.61418953 to 41.89538118,
      -89.61513367 to 41.89416727,
      -89.61393204 to 41.89320891,
    )

  @Test
  fun testPointSerialization() {
    val point = point(x, y)

    assertThat(point).isEqualTo(point.toLocalDataStoreObject().getGeometry())
  }

  @Test
  fun testPointIsEmpty() {
    val point = point(x, y)

    assertThat(point.isEmpty()).isEqualTo(false)
  }

  @Test
  fun testPointArea() {
    val point = point(x, y)
    assertThat(point.area).isEqualTo(0.0)
  }

  @Test
  fun testPolygonSerialization() {
    val polygon = polygon(path1, path2)

    assertThat(polygon).isEqualTo(polygon.toLocalDataStoreObject().getGeometry())
  }

  @Test
  fun testPolygonArea() {
    val polygon = polygon(path1, path2)

    assertThat(polygon.area).isEqualTo(4130.091187385864)
  }

  @Test
  fun testPolygonCenter() {
    val polygon = polygon(path1, path2)

    assertThat(polygon.center()).isEqualTo(Coordinates(lat = -89.633029365, lng = 41.89333657))
  }

  @Test
  fun testMultiPolygonSerialization() {
    val multiPolygon = multiPolygon(polygon(path1, path2), polygon(path3, path4))

    assertThat(multiPolygon).isEqualTo(multiPolygon.toLocalDataStoreObject().getGeometry())
  }

  @Test
  fun testMultiPolygonArea() {
    val multiPolygon = multiPolygon(polygon(path1, path2), polygon(path3, path4))

    assertThat(multiPolygon.area).isEqualTo(5953.0440156963905)
  }

  @Test
  fun testMultiPolygonCenter() {
    val multiPolygon = multiPolygon(polygon(path1, path2), polygon(path3, path4))

    assertThat(multiPolygon.center())
      .isEqualTo(Coordinates(lat = -89.62333049749999, lng = 41.89413522))
  }

  @Test
  fun testMultiPolygonIsEmpty() {
    val multiPolygon = multiPolygon(polygon(path1, path2), polygon(path3, path4))

    assertThat(multiPolygon.isEmpty()).isEqualTo(false)
  }

  @Test
  fun validateLinearRing_firstAndLastNotSame_throws() {
    Assert.assertThrows("Invalid linear ring", InvalidGeometryException::class.java) {
      LinearRing(OPEN_LOOP).validate()
    }
  }

  @Test
  fun validateLinearRing_empty_doesNothing() {
    LinearRing(listOf()).validate()
  }

  @Test
  fun validateLinearRing_firstAndLastSame_doesNothing() {
    LinearRing(CLOSED_LOOP).validate()
  }

  @Test
  fun isComplete_whenCountIsLessThanFour() {
    assertThat(LineString(OPEN_LOOP).isClosed()).isFalse()
  }

  @Test
  fun isComplete_whenCountIsFour_whenFirstAndLastAreNotSame() {
    assertThat(LineString(OPEN_LOOP + COORDINATE_4).isClosed()).isFalse()
  }

  @Test
  fun isComplete_whenCountIsFour_whenFirstAndLastAreSame() {
    assertThat(LineString(CLOSED_LOOP).isClosed()).isTrue()
  }

  @Test
  fun testCenterOfLineString() {
    assertThat(LineString(CLOSED_LOOP).center()).isEqualTo(COORDINATE_2)
    assertThat(LineString(OPEN_LOOP).center()).isEqualTo(COORDINATE_2)
  }

  @Test
  fun testAreaOfLineString() {
    assertThat(LineString(CLOSED_LOOP).area).isEqualTo(0.0)
  }

  private fun point(x: Double, y: Double) = Point(Coordinates(x, y))

  private fun linearRing(path: Path) = LinearRing(toCoordinateList(path))

  private fun polygon(shell: Path, vararg holes: Path) =
    Polygon(linearRing(shell), holes.map(::linearRing))

  private fun multiPolygon(vararg polygons: Polygon) = MultiPolygon(polygons.asList())

  private fun toCoordinateList(path: Path): List<Coordinates> =
    path.map { Coordinates(it.first, it.second) }

  companion object {
    private val COORDINATE_1 = Coordinates(10.0, 10.0)
    private val COORDINATE_2 = Coordinates(20.0, 20.0)
    private val COORDINATE_3 = Coordinates(30.0, 30.0)
    private val COORDINATE_4 = Coordinates(40.0, 40.0)

    private val OPEN_LOOP = listOf(COORDINATE_1, COORDINATE_2, COORDINATE_3)
    private val CLOSED_LOOP = listOf(COORDINATE_1, COORDINATE_2, COORDINATE_3, COORDINATE_1)
  }
}
