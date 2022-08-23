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

import com.google.common.collect.ImmutableList

/**
 * Represents types of geometry errors.
 *
 * Typically thrown when a construction does not satisfy definitional constraints for a given geometry.
 */
sealed class GeometryException(override val message: String) : Throwable(message)

class OpenLinearRingException(first: Coordinate, last: Coordinate) :
    GeometryException(
        """Invalid linear ring. Linear rings must be closed, but the first coordinate
        |$first and last coordinate $last of the ring are not equal""".trimMargin()
    )

class LinearRingLengthException(numVertices: Int) :
    GeometryException("Invalid linear ring. Expected 3 or more vertices but got $numVertices")

class LineStringLengthException(numVertices: Int) :
    GeometryException("Invalid line string. Expected 2 or more vertices but got: $numVertices")

class PolygonExteriorHoleException(val shellMaximum: Coordinate, val holeMaximum: Coordinate) :
    GeometryException(
        """Invalid polygon. Holes must be contained within a polygon's shell, but the
        |maximum coordinates of the shell $shellMaximum are lesser than the maximum coordinates of a
        |hole $holeMaximum""".trimMargin()
    )

sealed interface Geometry {
    /** Returns a primary [Coordinate] associated with this geometry. */
    val coordinate: Coordinate

    /** Returns a list of [Coordinate]s associated with this geometry. */
    val coordinates: ImmutableList<Coordinate>
}