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

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.locationtech.jts.geom.*

const val MULTI_POLYGON_GEOJSON =
    "{type=MultiPolygon, coordinates={0={0={0=GeoPoint { latitude=-89.63410225, longitude=41.89729784 }, 1=GeoPoint { latitude=-89.63805046, longitude=41.89525341 }, 2=GeoPoint { latitude=-89.63659134, longitude=41.8893753 }, 3=GeoPoint { latitude=-89.62886658, longitude=41.88956699 }, 4=GeoPoint { latitude=-89.62800827, longitude=41.89544508 }, 5=GeoPoint { latitude=-89.63410225, longitude=41.89729784 }}, 1={0=GeoPoint { latitude=-89.63453141, longitude=41.89193107 }, 1=GeoPoint { latitude=-89.63118401, longitude=41.89090877 }, 2=GeoPoint { latitude=-89.63066903, longitude=41.89397561 }, 3=GeoPoint { latitude=-89.63358727, longitude=41.89480618 }, 4=GeoPoint { latitude=-89.63453141, longitude=41.89193107 }}}, 1={0={0=GeoPoint { latitude=-89.61006966, longitude=41.8933367 }, 1=GeoPoint { latitude=-89.61479035, longitude=41.89832003 }, 2=GeoPoint { latitude=-89.61719361, longitude=41.89455062 }, 3=GeoPoint { latitude=-89.6152195, longitude=41.89154771 }, 4=GeoPoint { latitude=-89.61006966, longitude=41.8933367 }}, 1={0=GeoPoint { latitude=-89.61393204, longitude=41.89320891 }, 1=GeoPoint { latitude=-89.61290207, longitude=41.89429506 }, 2=GeoPoint { latitude=-89.61418953, longitude=41.89538119 }, 3=GeoPoint { latitude=-89.61513367, longitude=41.89416728 }, 4=GeoPoint { latitude=-89.61393204, longitude=41.89320891 }}}}}"

class GeometryConverterTest {
    private val converter = GeometryConverter()
    lateinit var point: Point
    lateinit var multiPolygon: MultiPolygon

    @Before
    fun setUp() {
        val geometryFactory = GeometryFactory()
        val ring1 = geometryFactory.createLinearRing(
            arrayOf(
                Coordinate(-89.63410225291251, 41.897297841913904),
                Coordinate(-89.63805046458243, 41.895253409968504),
                Coordinate(-89.63659134287833, 41.889375303577744),
                Coordinate(-89.62886658091544, 41.88956698949209),
                Coordinate(-89.62800827403068, 41.89544507824354),
                Coordinate(-89.63410225291251, 41.897297841913904)
            )
        )
        val ring2 = geometryFactory.createLinearRing(
            arrayOf(
                Coordinate(-89.6345314063549, 41.89193106847542),
                Coordinate(-89.63118400950431, 41.890908774787086),
                Coordinate(-89.63066902537345, 41.893975606768805),
                Coordinate(-89.63358726878165, 41.89480618175658),
                Coordinate(-89.6345314063549, 41.89193106847542)
            )
        )
        val ring3 = geometryFactory.createLinearRing(
            arrayOf(
                Coordinate(-89.61006966013908, 41.89333669558227),
                Coordinate(-89.61479034800529, 41.89832003334451),
                Coordinate(-89.61719360728263, 41.89455062137226),
                Coordinate(-89.61521950144767, 41.891547710259616),
                Coordinate(-89.61006966013908, 41.89333669558227)
            )
        )
        val ring4 = geometryFactory.createLinearRing(
            arrayOf(
                Coordinate(-89.61393204112052, 41.89320891257806),
                Coordinate(-89.6129020728588, 41.89429505996539),
                Coordinate(-89.61418953318595, 41.89538118888245),
                Coordinate(-89.61513367075919, 41.89416727887849),
                Coordinate(-89.61393204112052, 41.89320891257806)
            )
        )
        val polygon1 =
            geometryFactory.createPolygon(ring1, arrayOf(ring2))
        val polygon2 =
            geometryFactory.createPolygon(ring3, arrayOf(ring4))
        point = geometryFactory.createPoint(Coordinate(42.0, 28.0))
        multiPolygon = geometryFactory.createMultiPolygon(arrayOf(polygon1, polygon2))
    }

    @Test
    fun toFirestoreMap_point() {
        assertEquals(
            "{type=Point, coordinates=GeoPoint { latitude=42.0, longitude=28.0 }}",
            converter.toFirestoreMap(point).toString()
        )
    }

    @Test
    fun toFirestoreMap_multiPolygon() {
        assertEquals(
            MULTI_POLYGON_GEOJSON,
            converter.toFirestoreMap(multiPolygon).toString()
        )
    }
}