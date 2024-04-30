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

import com.google.android.ground.assertIsFailure
import com.google.android.ground.assertIsSuccessWith
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.firebase.firestore.GeoPoint
import org.junit.Test

typealias Path = Array<Pair<Double, Double>>

class GeometryConverterTest {
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
  fun toFirestoreMap_point() {
    assertIsSuccessWith(
      mapOf("type" to "Point", "coordinates" to GeoPoint(x, y)),
      GeometryConverter.toFirestoreMap(point(x, y)),
    )
  }

  @Test
  fun toFirestoreMap_polygon() {
    assertIsSuccessWith(
      mapOf(
        "type" to "Polygon",
        "coordinates" to mapOf("0" to indexedGeoPointMap(path1), "1" to indexedGeoPointMap(path2)),
      ),
      GeometryConverter.toFirestoreMap(polygon(path1, path2)),
    )
  }

  @Test
  fun toFirestoreMap_multiPolygon() {
    assertIsSuccessWith(
      mapOf(
        "type" to "MultiPolygon",
        "coordinates" to
          mapOf(
            "0" to mapOf("0" to indexedGeoPointMap(path1), "1" to indexedGeoPointMap(path2)),
            "1" to mapOf("0" to indexedGeoPointMap(path3), "1" to indexedGeoPointMap(path4)),
          ),
      ),
      GeometryConverter.toFirestoreMap(multiPolygon(polygon(path1, path2), polygon(path3, path4))),
    )
  }

  @Test
  fun fromFirestoreMap_point() {
    assertIsSuccessWith(
      point(x, y),
      GeometryConverter.fromFirestoreMap(mapOf("type" to "Point", "coordinates" to GeoPoint(x, y))),
    )
  }

  @Test
  fun fromFirestoreMap_nullGeometry() {
    assertIsFailure(GeometryConverter.fromFirestoreMap(null))
  }

  @Test
  fun fromFirestoreMap_nullCoordinates() {
    assertIsFailure(
      GeometryConverter.fromFirestoreMap(mapOf("type" to "Point", "coordinates" to null))
    )
  }

  @Test
  fun fromFirestoreMap_nullNestedElement() {
    assertIsFailure(
      GeometryConverter.fromFirestoreMap(
        mapOf("type" to "MultiPolygon", "coordinates" to mapOf(0 to mapOf(0 to null, 1 to null)))
      )
    )
  }

  @Test
  fun fromFirestoreMap_invalidGeometryType() {
    assertIsFailure(GeometryConverter.fromFirestoreMap(mapOf("type" to 123.0)))
  }

  @Test
  fun fromFirestoreMap_missingCoordinates() {
    assertIsFailure(GeometryConverter.fromFirestoreMap(mapOf("type" to "Point")))
  }

  @Test
  fun fromFirestoreMap_polygon() {
    assertIsSuccessWith(
      polygon(path1, path2),
      GeometryConverter.fromFirestoreMap(
        mapOf(
          "type" to "Polygon",
          "coordinates" to mapOf("0" to indexedGeoPointMap(path1), "1" to indexedGeoPointMap(path2)),
        )
      ),
    )
  }

  @Test
  fun fromFirestoreMap_polygon_nonSequentialIndices() {
    assertIsFailure(
      GeometryConverter.fromFirestoreMap(
        mapOf(
          "type" to "Polygon",
          "coordinates" to mapOf("0" to indexedGeoPointMap(path1), "2" to indexedGeoPointMap(path2)),
        )
      )
    )
  }

  @Test
  fun fromFirestoreMap_polygon_nonZeroBasedIndices() {
    assertIsFailure(
      GeometryConverter.fromFirestoreMap(
        mapOf(
          "type" to "Polygon",
          "coordinates" to mapOf("1" to indexedGeoPointMap(path1), "2" to indexedGeoPointMap(path2)),
        )
      )
    )
  }

  @Test
  fun fromFirestoreMap_polygon_invalidIndexType() {
    assertIsFailure(
      GeometryConverter.fromFirestoreMap(
        mapOf(
          "type" to "Polygon",
          "coordinates" to mapOf("1" to indexedGeoPointMap(path1), "2" to indexedGeoPointMap(path2)),
        )
      )
    )
  }

  @Test
  fun fromFirestoreMap_multiPolygon() {
    assertIsSuccessWith(
      multiPolygon(polygon(path1, path2), polygon(path3, path4)),
      GeometryConverter.fromFirestoreMap(
        mapOf(
          "type" to "MultiPolygon",
          "coordinates" to
            mapOf(
              "0" to mapOf("0" to indexedGeoPointMap(path1), "1" to indexedGeoPointMap(path2)),
              "1" to mapOf("0" to indexedGeoPointMap(path3), "1" to indexedGeoPointMap(path4)),
            ),
        )
      ),
    )
  }

  private fun point(x: Double, y: Double) = Point(Coordinates(x, y))

  private fun linearRing(path: Path) = LinearRing(toCoordinateList(path))

  private fun polygon(shell: Path, vararg holes: Path) =
    Polygon(linearRing(shell), holes.map(::linearRing))

  private fun multiPolygon(vararg polygons: Polygon) = MultiPolygon(polygons.asList())

  private fun toCoordinateList(path: Path): List<Coordinates> =
    path.map { Coordinates(it.first, it.second) }

  private fun indexedGeoPointMap(path: Path): Map<String, Any> =
    path.mapIndexed { idx, (first, second) -> idx.toString() to GeoPoint(first, second) }.toMap()
}
