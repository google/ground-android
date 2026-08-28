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
package org.groundplatform.domain.model.map

import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Geometry

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

  /** Returns the center coordinates of these bounds. */
  val center: Coordinates
    get() {
      val centerLat = (south + north) / 2.0
      val centerLng =
        if (west <= east) {
          (west + east) / 2.0
        } else {
          var lng = (west + east + 360.0) / 2.0
          if (lng > 180.0) lng -= 360.0
          lng
        }
      return Coordinates(centerLat, centerLng)
    }

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

  /** Returns true if the given [Coordinates] is within these bounds. */
  fun contains(coordinates: Coordinates): Boolean {
    val lat = coordinates.lat
    val lng = coordinates.lng
    val latInRange = lat in south..north
    val lngInRange = if (west <= east) lng in west..east else lng >= west || lng <= east
    return latInRange && lngInRange
  }

  /** Returns true if any vertex of the given [Geometry] is within these bounds. */
  fun contains(geometry: Geometry): Boolean = geometry.getShellCoordinates().any { contains(it) }

  /** Returns the center coordinates of these bounds. */
  fun center(): Coordinates = center

  companion object {
    /**
     * Returns a [Bounds] enclosing all the given coordinates, or null if the collection is empty.
     *
     * Expands longitude using the minimal longitudinal span, properly handling bounds that cross
     * the 180th meridian (anti-meridian).
     */
    fun fromCoordinates(coordinates: Iterable<Coordinates>): Bounds? {
      val iterator = coordinates.iterator()
      if (!iterator.hasNext()) return null
      val first = iterator.next()
      var minLat = first.lat
      var maxLat = first.lat
      var west = first.lng
      var east = first.lng

      while (iterator.hasNext()) {
        val point = iterator.next()
        minLat = minOf(minLat, point.lat)
        maxLat = maxOf(maxLat, point.lat)
        val lng = point.lng

        val isContained = if (west <= east) lng in west..east else lng >= west || lng <= east
        if (!isContained) {
          val distWest = (west - lng).mod(360.0)
          val distEast = (lng - east).mod(360.0)
          if (distWest < distEast) {
            west = lng
          } else {
            east = lng
          }
        }
      }
      return Bounds(south = minLat, west = west, north = maxLat, east = east)
    }

    /** Returns a [Bounds] enclosing all geometries in the collection, or null if empty. */
    fun fromGeometries(geometries: Iterable<Geometry>): Bounds? =
      fromCoordinates(geometries.flatMap { it.getShellCoordinates() })
  }
}
