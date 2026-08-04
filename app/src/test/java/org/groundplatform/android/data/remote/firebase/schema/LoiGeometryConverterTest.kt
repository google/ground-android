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
package org.groundplatform.android.data.remote.firebase.schema

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.groundplatform.android.data.remote.DataStoreException
import org.groundplatform.android.data.remote.firebase.schema.GeometryConverter.toGeometry
import org.groundplatform.android.proto.Coordinates as CoordinatesProto
import org.groundplatform.android.proto.Geometry as GeometryProto
import org.groundplatform.android.proto.LinearRing as LinearRingProto
import org.groundplatform.android.proto.MultiPolygon as MultiPolygonProto
import org.groundplatform.android.proto.Point as PointProto
import org.groundplatform.android.proto.Polygon as PolygonProto
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.MultiPolygon
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.geometry.Polygon
import org.junit.Test

class LoiGeometryConverterTest {

  @Test
  fun `reads a point`() {
    val geometry = LoiGeometryConverter.toGeometry(pointMap(1.0, 2.0))

    assertThat(geometry).isEqualTo(Point(Coordinates(1.0, 2.0)))
  }

  @Test
  fun `reads a polygon`() {
    val map = polygonGeometryMap(SHELL)

    val geometry = LoiGeometryConverter.toGeometry(map)

    assertThat(geometry).isEqualTo(Polygon(LinearRing(SHELL.map { Coordinates(it[0], it[1]) })))
  }

  @Test
  fun `reads a polygon with holes`() {
    val map = polygonGeometryMap(SHELL, HOLE)

    val geometry = LoiGeometryConverter.toGeometry(map) as Polygon

    assertThat(geometry.holes).hasSize(1)
    assertThat(geometry.holes.first().coordinates).isEqualTo(HOLE.map { Coordinates(it[0], it[1]) })
  }

  @Test
  fun `reads a multi polygon`() {
    val map =
      mapOf(
        GeometryProto.MULTI_POLYGON_FIELD_NUMBER.toString() to
          mapOf(
            MultiPolygonProto.POLYGONS_FIELD_NUMBER.toString() to
              listOf(polygonMap(SHELL), polygonMap(HOLE))
          )
      )

    val geometry = LoiGeometryConverter.toGeometry(map) as MultiPolygon

    assertThat(geometry.polygons).hasSize(2)
  }

  @Test
  fun `accepts whole-number coordinates, which Firestore returns as Long`() {
    val map =
      mapOf(
        GeometryProto.POINT_FIELD_NUMBER.toString() to
          mapOf(
            PointProto.COORDINATES_FIELD_NUMBER.toString() to
              mapOf(
                CoordinatesProto.LATITUDE_FIELD_NUMBER.toString() to 1L,
                CoordinatesProto.LONGITUDE_FIELD_NUMBER.toString() to 2L,
              )
          )
      )

    assertThat(LoiGeometryConverter.toGeometry(map)).isEqualTo(Point(Coordinates(1.0, 2.0)))
  }

  @Test
  fun `fails on an unrecognized geometry type`() {
    assertFailsWith<DataStoreException> { LoiGeometryConverter.toGeometry(mapOf("99" to 1)) }
  }

  @Test
  fun `fails when the geometry field is missing`() {
    assertFailsWith<DataStoreException> { LoiGeometryConverter.toGeometry(null) }
  }

  @Test
  fun `fails when a coordinate is not a number`() {
    val map =
      mapOf(
        GeometryProto.POINT_FIELD_NUMBER.toString() to
          mapOf(
            PointProto.COORDINATES_FIELD_NUMBER.toString() to
              mapOf(
                CoordinatesProto.LATITUDE_FIELD_NUMBER.toString() to "not a number",
                CoordinatesProto.LONGITUDE_FIELD_NUMBER.toString() to 2.0,
              )
          )
      )

    assertFailsWith<DataStoreException> { LoiGeometryConverter.toGeometry(map) }
  }

  private fun pointMap(lat: Double, lng: Double) =
    mapOf(
      GeometryProto.POINT_FIELD_NUMBER.toString() to
        mapOf(PointProto.COORDINATES_FIELD_NUMBER.toString() to coordinatesMap(lat, lng))
    )

  private fun coordinatesMap(lat: Double, lng: Double) =
    mapOf(
      CoordinatesProto.LATITUDE_FIELD_NUMBER.toString() to lat,
      CoordinatesProto.LONGITUDE_FIELD_NUMBER.toString() to lng,
    )

  private fun ringMap(coordinates: List<List<Double>>) =
    mapOf(
      LinearRingProto.COORDINATES_FIELD_NUMBER.toString() to
        coordinates.map { coordinatesMap(it[0], it[1]) }
    )

  private fun polygonMap(shell: List<List<Double>>, vararg holes: List<List<Double>>) = buildMap {
    put(PolygonProto.SHELL_FIELD_NUMBER.toString(), ringMap(shell))
    if (holes.isNotEmpty()) {
      put(PolygonProto.HOLES_FIELD_NUMBER.toString(), holes.map { ringMap(it) })
    }
  }

  private fun polygonGeometryMap(shell: List<List<Double>>, vararg holes: List<List<Double>>) =
    mapOf(GeometryProto.POLYGON_FIELD_NUMBER.toString() to polygonMap(shell, *holes))

  companion object {
    private val SHELL =
      listOf(listOf(0.0, 0.0), listOf(0.0, 1.0), listOf(1.0, 1.0), listOf(0.0, 0.0))
    private val HOLE =
      listOf(listOf(0.2, 0.2), listOf(0.2, 0.4), listOf(0.4, 0.4), listOf(0.2, 0.2))
  }
}
