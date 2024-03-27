/*
 * Copyright 2022 Google LLC
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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.firebase.firestore.GeoPoint

/** Alias for maps whose keys represent an index in an ordered data structure like a [List]. */
typealias IndexedMap<T> = Map<String, T>

/**
 * Converts between Geometry model objects and their equivalent remote representation using a
 * modified GeoJSON representation:
 * * The GeoJSON map hierarchy is converted to a Firestore nested map.
 * * Since Firestore does not allow nested arrays, arrays are replaced with nested maps, keyed by
 * * integer array index.
 * * Coordinates (two-element double arrays) are represented as a Firestore GeoPoint.
 *
 * Only `Point`, `Polygon`, and `MultiPolygon` are supported; behavior for other geometry types is
 * undefined.
 */
object GeometryConverter {
  private const val TYPE_KEY = "type"
  private const val COORDINATES_KEY = "coordinates"
  private const val POINT_TYPE = "Point"
  private const val POLYGON_TYPE = "Polygon"
  private const val MULTI_POLYGON_TYPE = "MultiPolygon"

  /**
   * Returns the remote db representation of a `Geometry` object. Errors are always returned as a
   * `Result`; exceptions are never thrown.
   */
  fun toFirestoreMap(geometry: Geometry): Result<Map<String, Any>> =
    Result.runCatching {
      when (geometry) {
        is Point -> geometryMapOf(POINT_TYPE, getPointCoordinates(geometry))
        is Polygon -> geometryMapOf(POLYGON_TYPE, getPolygonCoordinates(geometry))
        is MultiPolygon -> geometryMapOf(MULTI_POLYGON_TYPE, getMultiPolygonCoordinates(geometry))
        else -> throw UnsupportedOperationException("Can't convert ${geometry.javaClass}")
      }
    }

  private fun geometryMapOf(type: String, coordinates: Any): Map<String, Any> =
    mapOf(TYPE_KEY to type, COORDINATES_KEY to coordinates)

  private fun getPointCoordinates(point: Point): GeoPoint = coordinateToGeoPoint(point.coordinates)

  private fun getPolygonCoordinates(polygon: Polygon): IndexedMap<IndexedMap<GeoPoint>> =
    listToIndexedMap((listOf(polygon.shell) + polygon.holes).map(::getLinearRingCoordinates))

  private fun getLinearRingCoordinates(linearRing: LinearRing): IndexedMap<GeoPoint> =
    listToIndexedMap(linearRing.coordinates.map(::coordinateToGeoPoint))

  private fun getMultiPolygonCoordinates(
    multiPolygon: MultiPolygon
  ): IndexedMap<IndexedMap<IndexedMap<GeoPoint>>> =
    listToIndexedMap(multiPolygon.polygons.map(this::getPolygonCoordinates))

  private fun coordinateToGeoPoint(coordinates: Coordinates): GeoPoint =
    GeoPoint(coordinates.lat, coordinates.lng)

  private fun <T> listToIndexedMap(list: List<T>): IndexedMap<T> =
    list.mapIndexed { index, value -> index.toString() to value }.toMap()

  /** Converts a `Map` deserialized from Firestore into a `Geometry` instance. */
  fun fromFirestoreMap(map: Map<String, *>?): Result<Geometry> =
    Result.runCatching { fromFirestoreGeometry(map?.get(TYPE_KEY), map?.get(COORDINATES_KEY)) }

  /**
   * If data is missing or of an unexpected type, this method may fail with [ClassCastException] or
   * [NullPointerException]. Callers are expected to handle these and propagate the invalid state
   * upstream accordingly.
   */
  @Suppress("UNCHECKED_CAST")
  private fun fromFirestoreGeometry(type: Any?, coordinates: Any?): Geometry =
    when (type) {
      POINT_TYPE -> geoPointToPoint(coordinates as GeoPoint)
      POLYGON_TYPE -> nestedIndexedMapToPolygon(coordinates as IndexedMap<IndexedMap<GeoPoint>>)
      MULTI_POLYGON_TYPE ->
        nestedIndexedMapToMultiPolygon(coordinates as IndexedMap<IndexedMap<IndexedMap<GeoPoint>>>)

      else -> throw DataStoreException("Invalid geometry type '$type'")
    }

  private fun geoPointToPoint(geoPoint: GeoPoint): Point = Point(geoPointToCoordinates(geoPoint))

  private fun geoPointToCoordinates(geoPoint: GeoPoint): Coordinates =
    Coordinates(geoPoint.latitude, geoPoint.longitude)

  private fun nestedIndexedMapToPolygon(ringsMap: IndexedMap<IndexedMap<GeoPoint>>): Polygon {
    val rings = indexedMapToList(ringsMap)
    return Polygon(
      indexedMapToLinearRing(rings.first()),
      rings.drop(1).map(this::indexedMapToLinearRing)
    )
  }

  private fun indexedMapToLinearRing(coordinatesMap: IndexedMap<GeoPoint>): LinearRing =
    LinearRing(indexedMapToList(coordinatesMap).map(this::geoPointToCoordinates))

  private fun nestedIndexedMapToMultiPolygon(
    coordinatesMap: IndexedMap<IndexedMap<IndexedMap<GeoPoint>>>
  ): Geometry = MultiPolygon(indexedMapToList(coordinatesMap).map(this::nestedIndexedMapToPolygon))

  /**
   * Converts map representation used to store nested arrays in Firestore into a List. Throws
   * [IllegalArgumentException] if keys aren't consecutive Ints starting from 0
   */
  private fun <T> indexedMapToList(map: IndexedMap<T>): List<T> {
    val sortedEntries = map.entries.sortedBy { it.key.toInt() }
    require(sortedEntries.withIndex().all { it.index == it.value.key.toInt() }) {
      "Invalid map $map"
    }
    return sortedEntries.map { it.value }
  }
}
