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
import org.locationtech.jts.geom.LineString

/** An open or closed line consisting of a series of [Point]s. */
data class PolyLine(val points: ImmutableList<Point>) : Geometry {
    private val factory: GeometryFactory = GeometryFactory()
    private val coordinates: Collection<Coordinate> = points.map { it.geometry.coordinate }

    override val geometry: LineString = factory.createLineString(coordinates.toTypedArray())
    override val type: GeometryType = GeometryType.POLYLINE
    override val isClosed: Boolean
        get() = geometry.isRing

    val vertices: ImmutableList<Point>
        get() = geometry.coordinates.mapNotNull { Point(it.x, it.y) }.toImmutableList()

    /** Returns the first point in this polygon's coordinates, or null if the polygon has no coordinates.*/
    val firstVertex: Point?
        get() = vertices.getOrNull(0)?.let { return it }

    fun toPolygon(): Polygon? = if (isClosed) {
        Polygon(points)
    } else {
        null
    }
}