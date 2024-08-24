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
import com.google.android.ground.proto.Geometry as GeometryProto
import com.google.android.ground.proto.LinearRing as LinearRingProto
import com.google.android.ground.proto.MultiPolygon as MultiPolygonProto
import com.google.android.ground.proto.Point as PointProto
import com.google.android.ground.proto.Polygon as PolygonProto

/**
 * Converts between Geometry model objects and their equivalent remote representation using protos.
 */
object GeometryConverter {

  private fun PointProto.toPoint() = Point(Coordinates(coordinates.latitude, coordinates.longitude))

  private fun LinearRingProto.toLinearRing() =
    LinearRing(coordinatesList.map { Coordinates(it.latitude, it.longitude) })

  private fun PolygonProto.toPolygon() =
    Polygon(shell.toLinearRing(), holesList.map { it.toLinearRing() })

  private fun MultiPolygonProto.toMultiPolygon() = MultiPolygon(polygonsList.map { it.toPolygon() })

  fun GeometryProto.toGeometry(): Geometry =
    when (geometryTypeCase) {
      GeometryProto.GeometryTypeCase.POINT -> point.toPoint()
      GeometryProto.GeometryTypeCase.POLYGON -> polygon.toPolygon()
      GeometryProto.GeometryTypeCase.MULTI_POLYGON -> multiPolygon.toMultiPolygon()
      else -> throw UnsupportedOperationException("Can't convert $geometryTypeCase")
    }
}
