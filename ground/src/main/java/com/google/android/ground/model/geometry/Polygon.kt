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

class PolygonExteriorHoleException(val shellMaximum: Coordinate, val holeMaximum: Coordinate) :
    GeometryException("Invalid polygon. Holes must be contained within a polygon's shell, but the maximum coordinates of the shell $shellMaximum are lesser than the maximum coordinates of a hole $holeMaximum")

/** A polygon made up of a linear ring that dictates its bounds and any number of holes within the shell ring.*/
data class Polygon(val shell: LinearRing, val holes: List<LinearRing>) : Geometry {
    init {
        holes.forEach {
            if (!shell.contains(it)) {
                throw PolygonExteriorHoleException(shell.maximum(), it.maximum())
            }
        }
    }

    /** Returns the coordinate of the first vertex in this polygon's shell. */
    override val coordinate: Coordinate = shell.coordinates.first()

    /** Returns all coordinates in this polygon's shell. */
    override val coordinates: ImmutableList<Coordinate> = shell.coordinates
}
