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

import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.locationtech.jts.geom.*

class GeometryConverterTest {
    val path1 = arrayOf(
        -89.63410225 to 41.89729784,
        -89.63805046 to 41.89525340,
        -89.63659134 to 41.88937530,
        -89.62886658 to 41.88956698,
        -89.62800827 to 41.89544507,
        -89.63410225 to 41.89729784
    )
    val path2 = arrayOf(
        -89.63453141 to 41.89193106,
        -89.63118400 to 41.89090878,
        -89.63066902 to 41.89397560,
        -89.63358726 to 41.89480618,
        -89.63453141 to 41.89193106
    )
    val path3 = arrayOf(
        -89.61006966 to 41.89333669,
        -89.61479034 to 41.89832003,
        -89.61719360 to 41.89455062,
        -89.61521950 to 41.89154771,
        -89.61006966 to 41.89333669
    )
    val path4 = arrayOf(
        -89.61393204 to 41.89320891,
        -89.61290207 to 41.89429505,
        -89.61418953 to 41.89538118,
        -89.61513367 to 41.89416727,
        -89.61393204 to 41.89320891
    )

    private val converter = GeometryConverter()
    lateinit var point: Point
    lateinit var multiPolygon: MultiPolygon

    @Before
    fun setUp() {
        val gf = GeometryFactory()
        val ring1 = gf.createLinearRing(toCoordinateArray(path1))
        val ring2 = gf.createLinearRing(toCoordinateArray(path2))
        val ring3 = gf.createLinearRing(toCoordinateArray(path3))
        val ring4 = gf.createLinearRing(toCoordinateArray(path4))
        val polygon1 = gf.createPolygon(ring1, arrayOf(ring2))
        val polygon2 = gf.createPolygon(ring3, arrayOf(ring4))
        point = gf.createPoint(Coordinate(42.0, 28.0))
        multiPolygon = gf.createMultiPolygon(arrayOf(polygon1, polygon2))
    }

    @Test
    fun toFirestoreMap_point() {
        assertEquals(
            mapOf(
                "type" to "Point",
                "coordinates" to GeoPoint(point.x, point.y)
            ),
            converter.toFirestoreMap(point)
        )
    }

    @Test
    fun toFirestoreMap_multiPolygon() {
        assertEquals(
            mapOf(
                "type" to "MultiPolygon",
                "coordinates" to mapOf(
                    0 to mapOf(
                        0 to toGeoPointMap(path1),
                        1 to toGeoPointMap(path2)
                    ),
                    1 to mapOf(
                        0 to toGeoPointMap(path3),
                        1 to toGeoPointMap(path4)
                    )
                )
            ),
            converter.toFirestoreMap(multiPolygon)
        )
    }

    private fun toCoordinateArray(path: Array<Pair<Double, Double>>): Array<Coordinate> =
        path.map { Coordinate(it.first, it.second) }.toTypedArray()

    private fun toGeoPointMap(path: Array<Pair<Double, Double>>): Map<Int, Any> =
        path.mapIndexed { idx, it -> idx to GeoPoint(it.first, it.second) }.toMap()
}