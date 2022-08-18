/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.model.locationofinterest

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Coordinate

/** The location of a single point on the map. */
data class Point(
    val latitude: Double,
    val longitude: Double
) {
    private val geometryFactory: GeometryFactory = GeometryFactory()

    fun toGeometry(): org.locationtech.jts.geom.Point =
        geometryFactory.createPoint(Coordinate(latitude, longitude))

    companion object {
        @JvmStatic
        fun fromCoordinate(coordinate: Coordinate?): Point {
            return if (coordinate == null) {
                zero()
            } else Point(coordinate.x, coordinate.y)
        }

        @JvmStatic
        fun zero(): Point = Point(0.0, 0.0)
    }
}