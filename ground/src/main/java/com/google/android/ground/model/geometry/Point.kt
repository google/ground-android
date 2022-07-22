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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point

/** A single point given by x and y coordinates in a Cartesian plane. */
data class Point(val x: Double, val y: Double) : Geometry {
    private val factory: GeometryFactory = GeometryFactory()
    private val coordinate: Coordinate = Coordinate(x, y)

    override val geometry: Point = factory.createPoint(coordinate)
    override val type: GeometryType = GeometryType.POINT
    override val isClosed = false

    val latitude: Double
        get() = geometry.x

    val longitude: Double
        get() = geometry.y

}