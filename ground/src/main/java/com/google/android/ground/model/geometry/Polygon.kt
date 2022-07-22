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

package com.google.android.ground.model.geometry

import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon

/** A closed polygon consisting of a series of [Point]s that form a linear ring.
 *
 *  For shapes that are not closed, use [PolyLine].
 * */
data class Polygon(val points: ImmutableList<Point>) : Geometry {
    // TODO: Add support for holes.
    private val factory: GeometryFactory = GeometryFactory()
    private val coordinates: Collection<Coordinate> = points.map { it.geometry.coordinate }

    override val geometry: Polygon = factory.createPolygon(coordinates.toTypedArray())
    override val type: GeometryType = GeometryType.POLYGON
    override val isClosed: Boolean = true

    val vertices: ImmutableList<Point>
        get() = geometry.coordinates.map { Point(it.x, it.y) }.toImmutableList()

    /** Returns the first point in this polygon's coordinates, or null if the polygon has no coordinates.*/
    val firstVertex: Point?
        get() {
            val coordinates = geometry.coordinates

            coordinates.getOrNull(0)?.let {
                return Point(it.x, it.y)
            }

            return null
        }

    /** Returns the last point in this polygon's coordinates, or null if the polygon has no coordinates.*/
    val lastVertex: Point?
        get() {
            val coordinates = geometry.coordinates

            coordinates.getOrNull(coordinates.lastIndex)?.let {
                return Point(it.x, it.y)
            }

            return null
        }
}