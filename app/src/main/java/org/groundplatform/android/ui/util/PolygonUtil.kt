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

/**
 * Converts degrees to radians.
 *
 * @param deg Angle in degrees.
 * @return The angle in radians.
 */
private fun toRadians(deg: Double): Double = deg * (Math.PI / 180.0)

private const val EARTH_RADIUS = 6378137.0 // Radius of Earth in meters

/**
 * Converts a geographic coordinate to planar meters relative to a reference point.
 *
 * Uses an equirectangular approximation with the Earth's radius.
 *
 * @param reference The reference [Coordinates] used as origin.
 * @param point The [Coordinates] to convert.
 * @return A pair (x, y) in meters relative to the reference.
 */
private fun toMeters(reference: Coordinates, point: Coordinates): Pair<Double, Double> {
  val dX =
    (point.lng - reference.lng) *
      EARTH_RADIUS *
      cos(toRadians((reference.lat + point.lat) / 2.0)) *
      (Math.PI / 180.0)
  val dY = (point.lat - reference.lat) * EARTH_RADIUS * (Math.PI / 180.0)
  return Pair(dX, dY)
}

/**
 * Checks if two line segments intersect.
 *
 * @param p1 First endpoint of the first segment.
 * @param p2 Second endpoint of the first segment.
 * @param q1 First endpoint of the second segment.
 * @param q2 Second endpoint of the second segment.
 * @return `true` if the segments intersect or overlap, `false` otherwise.
 */
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

/**
 * Determines the orientation of the ordered triplet (a, b, c).
 *
 * @return 1 if clockwise, -1 if counter-clockwise, 0 if collinear.
 */
private fun orientation(a: Coordinates, b: Coordinates, c: Coordinates): Int {
  val value = (b.lat - a.lat) * (c.lng - b.lng) - (b.lng - a.lng) * (c.lat - b.lat)
  return when {
    value > 0 -> 1 // Clockwise
    value < 0 -> -1 // Counter-clockwise
    else -> 0 // Collinear
  }
}

/** Checks whether point c lies on the line segment ab. */
private fun onSegment(a: Coordinates, b: Coordinates, c: Coordinates) =
  c.lat in minOf(a.lat, b.lat)..maxOf(a.lat, b.lat) &&
    c.lng in minOf(a.lng, b.lng)..maxOf(a.lng, b.lng)

/** Represents a line segment defined by two endpoints. */
private data class PolygonEdge(val a: Coordinates, val b: Coordinates)

/**
 * Returns whether the polygon represented by vertices is closed (i.e., its first and last vertices
 * are the same).
 */
private fun isClosed(vertices: List<Coordinates>): Boolean =
  vertices.size >= 4 && vertices.first() == vertices.last()

/** Checks if two segments share a common endpoint. */
private fun shareEndpoint(s1: PolygonEdge, s2: PolygonEdge): Boolean =
  s1.a == s2.a || s1.a == s2.b || s1.b == s2.a || s1.b == s2.b

/**
 * Builds a list of edges from the given polygon vertices.
 *
 * Each [PolygonEdge] connects two consecutive vertices in the list. If the polygon is closed (first
 * vertex == last vertex), an additional edge is added from the last vertex back to the first to
 * complete the loop.
 *
 * Examples:
 * - For an open polyline [A, B, C], edges = (A→B, B→C)
 * - For a closed polygon [A, B, C, A], edges = (A→B, B→C, C→A)
 *
 * @param vertices List of polygon vertices.
 * @return A list of [PolygonEdge]s that represent the polygon’s sides.
 */
private fun buildSegments(vertices: List<Coordinates>): List<PolygonEdge> {
  val n = vertices.size
  if (n < 2) return emptyList()

  val closed = isClosed(vertices)

  val edges = ArrayList<PolygonEdge>(if (closed) n else n - 1)

  // Create edges between each consecutive pair of vertices.
  for (i in 0 until n - 1) {
    edges += PolygonEdge(vertices[i], vertices[i + 1])
  }

  // If the polygon is closed, add an edge from the last vertex back to the first
  // to ensure the polygon forms a complete loop.
  if (closed) edges += PolygonEdge(vertices[n - 1], vertices[0])

  return edges
}

/**
 * Returns whether two edges in a polygon are adjacent.
 *
 * Adjacency rules:
 * - An edge is considered adjacent to itself (useful for "skip" checks).
 * - Consecutive indices are adjacent (e.g., 3 and 4).
 * - If the polygon is closed, the first and last edges are also adjacent.
 *
 * @param firstEdgeIndex Index of the first edge in the edges list.
 * @param secondEdgeIndex Index of the second edge in the edges list.
 * @param edgeCount Total number of edges.
 * @param isClosedPolygon Whether the polygon is closed (first vertex equals last vertex).
 */
private fun areAdjacent(
  firstEdgeIndex: Int,
  secondEdgeIndex: Int,
  edgeCount: Int,
  isClosedPolygon: Boolean,
): Boolean {
  if (firstEdgeIndex == secondEdgeIndex) return true
  if (abs(firstEdgeIndex - secondEdgeIndex) == 1) return true
  if (isClosedPolygon) {
    val last = edgeCount - 1
    if (
      (firstEdgeIndex == 0 && secondEdgeIndex == last) ||
        (secondEdgeIndex == 0 && firstEdgeIndex == last)
    ) {
      return true
    }
  }
  return false
}

/**
 * Checks if a polygon formed by the given vertices is self-intersecting.
 *
 * A polygon is self-intersecting if any pair of non-adjacent segments intersect.
 *
 * @param vertices The polygon vertices.
 * @return `true` if the polygon is self-intersecting, `false` otherwise.
 */
fun isSelfIntersecting(vertices: List<Coordinates>): Boolean {
  if (vertices.size < 4) return false

  val segments = buildSegments(vertices)
  val segmentSize = segments.size
  val closed = isClosed(vertices)

  for (i in 0 until segmentSize) {
    val s1 = segments[i]
    for (j in i + 1 until segmentSize) {
      val s2 = segments[j]

      // Skip pairs we should not test (adjacent or sharing an endpoint)
      if (areAdjacent(i, j, segmentSize, closed) || shareEndpoint(s1, s2)) continue

      if (isIntersecting(s1.a, s1.b, s2.a, s2.b)) return true
    }
  }
  return false
}
