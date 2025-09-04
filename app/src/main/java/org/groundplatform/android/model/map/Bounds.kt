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
package org.groundplatform.android.model.map

import org.groundplatform.android.model.geometry.Coordinates

/**
 * Represents a rectangular bound on a map. A bounds may be constructed using only southwest and
 * northeast coordinates.
 */
data class Bounds(val southwest: Coordinates, val northeast: Coordinates) {
  // Suppress false-positive on constructor order.
  @Suppress("detekt:ClassOrdering")
  constructor(
    south: Double,
    west: Double,
    north: Double,
    east: Double,
  ) : this(Coordinates(south, west), Coordinates(north, east))

  val north
    get() = northeast.lat

  val east
    get() = northeast.lng

  val south
    get() = southwest.lat

  val west
    get() = southwest.lng

  val northwest
    get() = Coordinates(north, west)

  val southeast
    get() = Coordinates(south, east)

  /**
   * The corners of the bounds in counterclockwise order starting from the northwestern most vertex.
   */
  val corners
    get() = listOf(northwest, southwest, southeast, northeast)

  /**
   * Reduce size of bounding box by the specified factor. The width and height are multiplied by the
   * given value to produce a new bounding box centered on the same centroid as the original.
   */
  fun shrink(factor: Double): Bounds {
    val latOffset = (north - south) * factor * 0.5
    val lngOffset = (east - west) * factor * 0.5
    return Bounds(
      Coordinates(south + latOffset, west + lngOffset),
      Coordinates(north - latOffset, east - lngOffset),
    )
  }
}
