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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/** A common ancestor for all geometry types. */
@Serializable
sealed interface Geometry {
  // TODO(#1246): Remove. Stick with concrete semantics; leave it to callers to discriminate
  // subclasses.
  val vertices: List<Point>

  val size: Int
    get() = vertices.size

  /** Validates that the current [Geometry] is well-formed. */
  fun validate() {
    // default no-op implementation
  }
}

/**
 * A polygon made up of a linear ring that dictates its bounds and any number of holes within the
 * shell ring.
 */
@Serializable
@SerialName("polygon")
data class Polygon(val shell: LinearRing, val holes: List<LinearRing> = listOf()) : Geometry {
  override val vertices: List<Point> = shell.vertices
}

/** Represents a single point. */
@Serializable
@SerialName("point")
data class Point(val coordinate: Coordinate) : Geometry {
  override val vertices: List<Point> = listOf(this)
}

/** A collection of [Polygon]s. */
@Serializable
@SerialName("multi_polygon")
data class MultiPolygon(val polygons: List<Polygon>) : Geometry {
  override val vertices: List<Point> = polygons.flatMap { it.vertices }
}

/** A sequence of two or more vertices modelling an OCG style line string. */
@Serializable
@SerialName("line_string")
data class LineString(val coordinates: List<Coordinate>) : Geometry {
  override val vertices: List<Point> = coordinates.map { Point(it) }
}

/**
 * A closed linear ring is a sequence of [Coordinate]s where the first and last coordinates are
 * equal.
 */
@Serializable
@SerialName("linear_ring")
data class LinearRing(val coordinates: List<Coordinate>) : Geometry {

  init {
    validate()
  }

  override val vertices: List<Point> = coordinates.map { Point(it) }

  override fun validate() {
    // TODO(#1647): Check for vertices count > 3
    if (coordinates.isEmpty()) {
      return
    }
    if (coordinates.firstOrNull() != coordinates.lastOrNull()) {
      error("Invalid linear ring")
    }
  }

  /**
   * Returns a *synthetic* coordinate containing the maximum x and y coordinate values of this ring.
   */
  private fun maximum(): Coordinate {
    val maximumLat = this.coordinates.maxOfOrNull { it.lat }
    val maximumLng = this.coordinates.maxOfOrNull { it.lng }

    return Coordinate(maximumLat ?: 0.0, maximumLng ?: 0.0)
  }

  /**
   * Returns a *synthetic* coordinate containing the minimum x and y coordinate values of this ring.
   */
  private fun minimum(): Coordinate {
    val minimumLat = this.coordinates.minOfOrNull { it.lat }
    val minimumLng = this.coordinates.minOfOrNull { it.lng }

    return Coordinate(minimumLat ?: 0.0, minimumLng ?: 0.0)
  }

  /**
   * Returns true if this linear ring contains another.
   *
   * This is based on enveloping each ring and is equivalent to JTS's Envelope.covers method.
   */
  fun contains(other: LinearRing) =
    this.maximum() >= other.maximum() && this.minimum() <= other.minimum()
}

val geometrySerializer = Json {
  SerializersModule {
    polymorphic(Geometry::class, Point::class, Point.serializer())
    polymorphic(Geometry::class, Polygon::class, Polygon.serializer())
    polymorphic(Geometry::class, MultiPolygon::class, MultiPolygon.serializer())
    polymorphic(Geometry::class, LineString::class, LineString.serializer())
    polymorphic(Geometry::class, LinearRing::class, LinearRing.serializer())
  }
}
