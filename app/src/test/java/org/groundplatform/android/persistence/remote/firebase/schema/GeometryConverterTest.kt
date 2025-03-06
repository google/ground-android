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

package org.groundplatform.android.persistence.remote.firebase.schema

import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.MultiPolygon
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.persistence.remote.firebase.schema.GeometryConverter.toGeometry
import org.groundplatform.android.proto.coordinates
import org.groundplatform.android.proto.geometry
import org.groundplatform.android.proto.linearRing
import org.groundplatform.android.proto.multiPolygon
import org.groundplatform.android.proto.point
import org.groundplatform.android.proto.polygon
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

  @Test
  fun `toGeometry converts point from proto`() {
    assertThat(
        geometry {
            point = point {
              coordinates = coordinates {
                latitude = x
                longitude = y
              }
            }
          }
          .toGeometry()
      )
      .isEqualTo(Point(coordinates = Coordinates(x, y)))
  }

  @Test
  fun `toGeometry converts polygon and multipolygon from proto`() {
    val testPolygon =
      Polygon(
        shell = LinearRing(coordinates = path1.map { Coordinates(it.first, it.second) }),
        holes = listOf(LinearRing(coordinates = path2.map { Coordinates(it.first, it.second) })),
      )
    val polygonProto = polygon {
      shell = linearRing {
        coordinates.addAll(
          path1.map {
            coordinates {
              latitude = it.first
              longitude = it.second
            }
          }
        )
      }
      holes.add(
        linearRing {
          coordinates.addAll(
            path2.map {
              coordinates {
                latitude = it.first
                longitude = it.second
              }
            }
          )
        }
      )
    }
    assertThat(geometry { polygon = polygonProto }.toGeometry()).isEqualTo(testPolygon)
    assertThat(geometry { multiPolygon = multiPolygon { polygons.add(polygonProto) } }.toGeometry())
      .isEqualTo(MultiPolygon(polygons = listOf(testPolygon)))
  }
}
