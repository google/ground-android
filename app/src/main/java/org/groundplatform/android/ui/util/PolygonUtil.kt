/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.util

import kotlin.math.abs
import kotlin.math.cos
import org.groundplatform.android.model.geometry.Coordinates

/**
 * Calculates the area of a polygon using the Shoelace formula.
 *
 * This function computes the area of a simple, non-self-intersecting polygon based on its vertex
 * coordinates. The first coordinate is used as a reference to convert all other points to meters.
 *
 * @param coordinates A list of [Coordinates] representing the vertices of the polygon. The list
 *   must contain at least three points; otherwise, the function returns 0.0.
 * @return The area of the polygon in square meters.
 */
fun calculateShoelacePolygonArea(coordinates: List<Coordinates>): Double {
  if (coordinates.size < 3) return 0.0

  val reference = coordinates[0]
  val points = coordinates.map { toMeters(reference, it) }

  var area = 0.0
  for (i in points.indices) {
    val j = (i + 1) % points.size
    area += points[i].first * points[j].second - points[j].first * points[i].second
  }

  return abs(area) / 2.0
}

private fun toRadians(deg: Double): Double = deg * (Math.PI / 180.0)

private const val EARTH_RADIUS = 6378137.0 // Radius of Earth in meters

private fun toMeters(reference: Coordinates, point: Coordinates): Pair<Double, Double> {
  val dX =
    (point.lng - reference.lng) *
      EARTH_RADIUS *
      cos(toRadians((reference.lat + point.lat) / 2.0)) *
      (Math.PI / 180.0)
  val dY = (point.lat - reference.lat) * EARTH_RADIUS * (Math.PI / 180.0)
  return Pair(dX, dY)
}

/** Checks if two line segments intersect. */
fun isIntersecting(p1: Coordinates, p2: Coordinates, q1: Coordinates, q2: Coordinates): Boolean {
  val o1 = orientation(p1, p2, q1)
  val o2 = orientation(p1, p2, q2)
  val o3 = orientation(q1, q2, p1)
  val o4 = orientation(q1, q2, p2)

  return (o1 != o2 && o3 != o4) ||
    (o1 == 0 && onSegment(p1, p2, q1)) ||
    (o2 == 0 && onSegment(p1, p2, q2)) ||
    (o3 == 0 && onSegment(q1, q2, p1)) ||
    (o4 == 0 && onSegment(q1, q2, p2))
}

private fun orientation(a: Coordinates, b: Coordinates, c: Coordinates): Int {
  val value = (b.lat - a.lat) * (c.lng - b.lng) - (b.lng - a.lng) * (c.lat - b.lat)
  return when {
    value > 0 -> 1 // Clockwise
    value < 0 -> -1 // Counter-clockwise
    else -> 0 // Collinear
  }
}

private fun onSegment(a: Coordinates, b: Coordinates, c: Coordinates) =
  c.lat in minOf(a.lat, b.lat)..maxOf(a.lat, b.lat) &&
    c.lng in minOf(a.lng, b.lng)..maxOf(a.lng, b.lng)

/** Checks if a polygon formed by the given vertices is self-intersecting. */
fun isSelfIntersecting(vertices: List<Coordinates>): Boolean {
  if (vertices.size < 4) return false // A polygon must have at least 4 points to self-intersect

  for (i in 0 until vertices.size - 1) {
    val segment1 = Pair(vertices[i], vertices[i + 1])

    for (j in i + 2 until vertices.size - 1) {
      val segment2 = Pair(vertices[j], vertices[j + 1])

      if (isIntersecting(segment1.first, segment1.second, segment2.first, segment2.second)) {
        return true
      }
    }
  }
  return false
}
