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

package com.google.android.ground.persistence.remote.firestore

import com.google.android.ground.model.geometry.*
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.firebase.firestore.GeoPoint

/**
 * Converts between Geometry model objects and their equivalent remote representation using a
 * modified GeoJSON representation:
 *
 * * The GeoJSON map hierarchy is converted to a Firestore nested map.
 * * Since Firestore does not allow nested arrays, arrays are replaced with nested maps, keyed by
 *   integer array index.
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
     * Returns the remote db representation of a `Geometry` object. Errors will always be returned as a `Result`;
     * exceptions are never thrown.
     */
    fun toFirestoreMap(geometry: Geometry): Result<Map<String, Any>> =
        Result.runCatching {
            when (geometry) {
                is Point -> geometryMap(POINT_TYPE, getPointCoordinates(geometry))
                is Polygon -> geometryMap(POLYGON_TYPE, getPolygonCoordinates(geometry))
                is MultiPolygon ->
                    geometryMap(
                        MULTI_POLYGON_TYPE,
                        getMultiPolygonCoordinates(geometry)

                    )
                else -> throw UnsupportedOperationException("Can't convert ${geometry.javaClass}")
            }
        }

    private fun geometryMap(type: String, coordinates: Any): Map<String, Any> =
        mapOf(
            TYPE_KEY to type,
            COORDINATES_KEY to coordinates
        )

    private fun getPolygonCoordinates(polygon: Polygon): Array<Array<GeoPoint>> =
        (listOf(polygon.shell) + polygon.holes).map(::getLinearRingCoordinates).toTypedArray()

    private fun getLinearRingCoordinates(linearRing: LinearRing): Array<GeoPoint> =
        linearRing.coordinates.map(this::toGeoPoint).toTypedArray()

    private fun getMultiPolygonCoordinates(multiPolygon: MultiPolygon): Array<Array<Array<GeoPoint>>> =
        multiPolygon.polygons.map(this::getPolygonCoordinates).toTypedArray()

    private fun toGeoPoint(coordinate: Coordinate): GeoPoint =
        GeoPoint(coordinate.x, coordinate.y)

    private fun getPointCoordinates(point: Point): GeoPoint = toGeoPoint(point.coordinate)

    /**
     * Converts a `Map` deserialized from Firestore into a `Geometry` instance.
     */
    fun fromFirestoreMap(map: Map<String, *>?): Result<Geometry> =
        Result.runCatching { fromFirestoreGeometry(map?.get(TYPE_KEY), map?.get(COORDINATES_KEY)) }

    private fun fromFirestoreGeometry(type: Any?, coordinates: Any?) =
        when (type) {
            POINT_TYPE -> fromPointCoordinates(coordinates)
            POLYGON_TYPE -> fromPolygonCoordinates(coordinates)
            MULTI_POLYGON_TYPE -> fromMultiPolygonCoordinates(coordinates)
            else -> throw DataStoreException("Invalid geometry type '$type'")
        }

    private fun fromPointCoordinates(coordinates: Any?): Point =
        Point(fromGeoPoint(coordinates))

    private fun fromPolygonCoordinates(coordinates: Any?): Polygon =
        if (coordinates == null || coordinates !is Array<*> || coordinates.isEmpty()) {
            throw DataStoreException("Bad polygon coordinates $coordinates")
        } else {
            Polygon(
                fromLinearRingCoordinates(coordinates.first()),
                coordinates.drop(1).map(this::fromLinearRingCoordinates)
            )
        }

    private fun fromMultiPolygonCoordinates(coordinates: Any?): Geometry =
        if (coordinates == null || coordinates !is Array<*> || coordinates.isEmpty()) {
            throw DataStoreException("Bad multi-polygon coordinates $coordinates")
        } else {
            MultiPolygon(coordinates.map(this::fromPolygonCoordinates))
        }

    private fun fromLinearRingCoordinates(coordinates: Any?): LinearRing =
        if (coordinates == null || coordinates !is Array<*> || coordinates.isEmpty()) {
            throw DataStoreException("Bad linear ring coordinates $coordinates")
        } else {
            LinearRing(coordinates.map(this::fromGeoPoint))
        }


    private fun fromGeoPoint(coordinates: Any?): Coordinate =
        if (coordinates == null || coordinates !is GeoPoint) {
            throw DataStoreException("Bad point coordinates $coordinates")
        } else {
            Coordinate(coordinates.latitude, coordinates.longitude)
        }


}