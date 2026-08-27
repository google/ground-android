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

import org.groundplatform.android.proto.Coordinates as CoordinatesProto
import org.groundplatform.android.proto.Geometry as GeometryProto
import org.groundplatform.android.proto.LinearRing as LinearRingProto
import org.groundplatform.android.proto.MultiPolygon as MultiPolygonProto
import org.groundplatform.android.proto.Point as PointProto
import org.groundplatform.android.proto.Polygon as PolygonProto
import org.groundplatform.data.DataStoreException
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Geometry
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.MultiPolygon
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.geometry.Polygon

// Keys are proto field numbers, as stored in Firestore. Derived from the generated constants so
// they stay correct if the schema is renumbered.
private val POINT = GeometryProto.POINT_FIELD_NUMBER.toString()
private val POLYGON = GeometryProto.POLYGON_FIELD_NUMBER.toString()
private val MULTI_POLYGON = GeometryProto.MULTI_POLYGON_FIELD_NUMBER.toString()
private val LATITUDE = CoordinatesProto.LATITUDE_FIELD_NUMBER.toString()
private val LONGITUDE = CoordinatesProto.LONGITUDE_FIELD_NUMBER.toString()
private val POINT_COORDINATES = PointProto.COORDINATES_FIELD_NUMBER.toString()
private val RING_COORDINATES = LinearRingProto.COORDINATES_FIELD_NUMBER.toString()
private val SHELL = PolygonProto.SHELL_FIELD_NUMBER.toString()
private val HOLES = PolygonProto.HOLES_FIELD_NUMBER.toString()
private val POLYGONS = MultiPolygonProto.POLYGONS_FIELD_NUMBER.toString()

/**
 * Builds [Geometry] straight from the nested maps of a Firestore document. Direct Firestore
 * geometry parsing avoids expensive reflection overhead.
 */
internal object LoiGeometryConverter {

  /** Converts the value of an LOI's geometry field. Throws [DataStoreException] if malformed. */
  fun toGeometry(value: Any?): Geometry {
    val geometry = value.orThrow<Map<*, *>>()
    val point = geometry[POINT]
    val polygon = geometry[POLYGON]
    val multiPolygon = geometry[MULTI_POLYGON]
    return when {
      point != null -> Point(point.orThrow<Map<*, *>>()[POINT_COORDINATES].toCoordinates())
      polygon != null -> polygon.toPolygon()
      multiPolygon != null ->
        MultiPolygon(
          multiPolygon.orThrow<Map<*, *>>()[POLYGONS].orThrow<List<*>>().map { it.toPolygon() }
        )
      else -> throw DataStoreException("Unrecognized geometry type: ${geometry.keys}")
    }
  }

  private fun Any?.toPolygon(): Polygon {
    val polygon = orThrow<Map<*, *>>()
    return Polygon(
      polygon[SHELL].toLinearRing(),
      polygon[HOLES]?.orThrow<List<*>>()?.map { it.toLinearRing() } ?: listOf(),
    )
  }

  private fun Any?.toLinearRing() =
    LinearRing(orThrow<Map<*, *>>()[RING_COORDINATES].orThrow<List<*>>().map { it.toCoordinates() })

  private fun Any?.toCoordinates(): Coordinates {
    val coordinates = orThrow<Map<*, *>>()
    return Coordinates(
      coordinates[LATITUDE].orThrow<Number>().toDouble(),
      coordinates[LONGITUDE].orThrow<Number>().toDouble(),
    )
  }

  private inline fun <reified T> Any?.orThrow(): T =
    this as? T ?: throw DataStoreException("Expected ${T::class.simpleName} but got $this")
}
