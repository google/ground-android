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

import kotlin.math.PI
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

  return abs(
    points.indices.sumOf { i ->
      val j = (i + 1) % points.size
      points[i].first * points[j].second - points[j].first * points[i].second
    }
  ) / 2.0
}

/** Converts geographic coordinate to meters relative to reference point. */
private fun toMeters(reference: Coordinates, point: Coordinates): Pair<Double, Double> {
  val earthRadius = 6378137.0
  val toRad = PI / 180.0
  val avgLat = (reference.lat + point.lat) / 2.0

  val dX = (point.lng - reference.lng) * earthRadius * cos(avgLat * toRad) * toRad
  val dY = (point.lat - reference.lat) * earthRadius * toRad
  return Pair(dX, dY)
}

/**
 * Checks if a polygon is self-intersecting.
 *
 * @param vertices Polygon vertices
 * @return true if self-intersecting
 */
fun isSelfIntersecting(vertices: List<Coordinates>): Boolean {
  if (vertices.size < 4) return false

  val edges = buildEdges(vertices)

  for (i in edges.indices) {
    for (j in i + 2 until edges.size) {
      // Skip if edges are adjacent (including wrap-around for closed polygons)
      val isAdjacent = j == i + 1 || (j == edges.size - 1 && i == 0 && isClosed(vertices))
      if (isAdjacent) continue

      if (segmentsIntersect(edges[i], edges[j])) return true
    }
  }
  return false
}

/** Builds edges from vertices, handling both open and closed polygons. */
private fun buildEdges(vertices: List<Coordinates>): List<Pair<Coordinates, Coordinates>> {
  val closed = isClosed(vertices)
  val points = if (closed) vertices.dropLast(1) else vertices

  return points
    .mapIndexed { i, point ->
      val nextIndex = if (closed) (i + 1) % points.size else i + 1
      if (nextIndex < points.size) point to points[nextIndex] else null
    }
    .filterNotNull()
}

/** Checks if polygon is closed (first == last vertex). */
fun isClosed(coordinates: List<Coordinates>): Boolean =
  coordinates.size >= 4 && coordinates.first() == coordinates.last()

/** Checks if two line segments intersect. */
fun segmentsIntersect(
  seg1: Pair<Coordinates, Coordinates>,
  seg2: Pair<Coordinates, Coordinates>,
): Boolean {
  val (p1, p2) = seg1
  val (q1, q2) = seg2

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

/**
 * Determines orientation of ordered triplet (a, b, c).
 *
 * @return 1 if clockwise, -1 if counter-clockwise, 0 if collinear
 */
private fun orientation(a: Coordinates, b: Coordinates, c: Coordinates): Int {
  val value = (b.lat - a.lat) * (c.lng - b.lng) - (b.lng - a.lng) * (c.lat - b.lat)
  return when {
    value > 0 -> 1
    value < 0 -> -1
    else -> 0
  }
}

/** Checks if point c lies on line segment ab. */
private fun onSegment(a: Coordinates, b: Coordinates, c: Coordinates): Boolean =
  c.lat in minOf(a.lat, b.lat)..maxOf(a.lat, b.lat) &&
    c.lng in minOf(a.lng, b.lng)..maxOf(a.lng, b.lng)
