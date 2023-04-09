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

import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.model.geometry.GeometryValidator.Companion.validate
import com.google.android.ground.ui.map.gms.toLatLng
import kotlinx.serialization.Serializable

/** A common ancestor for all geometry types. */
@Serializable
sealed interface Geometry {
  // TODO(#1246): Remove. Stick with concrete semantics; leave it to callers to discriminate
  // subclasses.
  val vertices: List<Point>

  val size: Int
    get() = vertices.size

  /** Returns true if the current geometry is within provided [bounds]. */
  fun isWithinBounds(bounds: LatLngBounds) = vertices.any { bounds.contains(it.toLatLng()) }
}

/**
 * A polygon made up of a linear ring that dictates its bounds and any number of holes within the
 * shell ring.
 */
@Serializable
data class Polygon(val shell: LinearRing, val holes: List<LinearRing> = listOf()) : Geometry {
  override val vertices: List<Point> = shell.vertices
}

/** Represents a single point. */
@Serializable
data class Point(val coordinate: Coordinate) : Geometry {
  override val vertices: List<Point> = listOf(this)
}

/** A collection of [Polygon]s. */
@Serializable
data class MultiPolygon(val polygons: List<Polygon>) : Geometry {
  override val vertices: List<Point> = polygons.flatMap { it.vertices }
}

/** A sequence of two or more vertices modelling an OCG style line string. */
@Serializable
data class LineString(val coordinates: List<Coordinate>) : Geometry {
  override val vertices: List<Point> = coordinates.map { Point(it) }
}

/**
 * A closed linear ring is a sequence of [Coordinate]s where the first and last coordinates are
 * equal.
 */
@Serializable
data class LinearRing(val coordinates: List<Coordinate>) : Geometry {

  init {
    validate()
  }

  override val vertices: List<Point> = coordinates.map { Point(it) }

  /**
   * Returns a *synthetic* coordinate containing the maximum x and y coordinate values of this ring.
   */
  private fun maximum(): Coordinate {
    val maximumX = this.coordinates.maxOfOrNull { it.x }
    val maximumY = this.coordinates.maxOfOrNull { it.y }

    return Coordinate(maximumX ?: 0.0, maximumY ?: 0.0)
  }

  /**
   * Returns a *synthetic* coordinate containing the minimum x and y coordinate values of this ring.
   */
  private fun minimum(): Coordinate {
    val minimumX = this.coordinates.minOfOrNull { it.x }
    val minimumY = this.coordinates.minOfOrNull { it.y }

    return Coordinate(minimumX ?: 0.0, minimumY ?: 0.0)
  }

  /**
   * Returns true if this linear ring contains another.
   *
   * This is based on enveloping each ring and is equivalent to JTS's Envelope.covers method.
   */
  fun contains(other: LinearRing) =
    this.maximum() >= other.maximum() && this.minimum() <= other.minimum()
}
