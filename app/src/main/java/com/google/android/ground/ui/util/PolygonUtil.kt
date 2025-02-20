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
package com.google.android.ground.ui.util

import com.google.android.ground.model.geometry.Coordinates

/** Checks if two line segments intersect. */
fun segmentsIntersect(p1: Coordinates, p2: Coordinates, q1: Coordinates, q2: Coordinates): Boolean {
  fun orientation(a: Coordinates, b: Coordinates, c: Coordinates): Int {
    val value = (b.lat - a.lat) * (c.lng - b.lng) - (b.lng - a.lng) * (c.lat - b.lat)
    return when {
      value > 0 -> 1 // Clockwise
      value < 0 -> -1 // Counter-clockwise
      else -> 0 // Collinear
    }
  }

  fun onSegment(a: Coordinates, b: Coordinates, c: Coordinates): Boolean {
    return c.lat in minOf(a.lat, b.lat)..maxOf(a.lat, b.lat) &&
      c.lng in minOf(a.lng, b.lng)..maxOf(a.lng, b.lng)
  }

  val o1 = orientation(p1, p2, q1)
  val o2 = orientation(p1, p2, q2)
  val o3 = orientation(q1, q2, p1)
  val o4 = orientation(q1, q2, p2)

  // General case: segments intersect
  if (o1 != o2 && o3 != o4) return true

  // Special cases: Collinear points
  if (o1 == 0 && onSegment(p1, p2, q1)) return true
  if (o2 == 0 && onSegment(p1, p2, q2)) return true
  if (o3 == 0 && onSegment(q1, q2, p1)) return true
  if (o4 == 0 && onSegment(q1, q2, p2)) return true

  return false
}
