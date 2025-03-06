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
package org.groundplatform.android.model.geometry

import com.google.maps.android.SphericalUtil.computeArea
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.groundplatform.android.ui.map.gms.GmsExt.center
import org.groundplatform.android.ui.map.gms.GmsExt.toBounds
import org.groundplatform.android.ui.map.gms.toLatLngList

/** A common ancestor for all geometry types. */
@Serializable
sealed interface Geometry {
  val area: Double

  /**
   * Returns the center coordinates of the geometry. It may or may not be within the geometry bounds
   * if the shape is irregular.
   */
  fun center(): Coordinates

  /** Returns true if there are one or more vertices in the geometry. */
  fun isEmpty(): Boolean

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
  override val area: Double
    get() =
      computeArea(shell.coordinates.toLatLngList()) -
        holes.sumOf { computeArea(it.coordinates.toLatLngList()) }

  override fun center(): Coordinates = shell.center()

  override fun isEmpty() = shell.isEmpty()

  fun getShellCoordinates() = shell.coordinates
}

/** Represents a single point. */
@Serializable
@SerialName("point")
data class Point(val coordinates: Coordinates) : Geometry {
  override val area: Double
    get() = 0.0

  override fun center(): Coordinates = coordinates

  override fun isEmpty() = false
}

/** A collection of [Polygon]s. */
@Serializable
@SerialName("multi_polygon")
data class MultiPolygon(val polygons: List<Polygon>) : Geometry {
  override val area: Double
    get() = polygons.sumOf { it.area }

  override fun center(): Coordinates = polygons.map { it.center() }.centerOrError()

  override fun isEmpty() = polygons.all { it.isEmpty() }
}

/** A sequence of two or more vertices modelling an OCG style line string. */
@Serializable
@SerialName("line_string")
data class LineString(val coordinates: List<Coordinates>) : Geometry {
  override val area: Double
    get() = 0.0

  override fun center(): Coordinates = coordinates.centerOrError()

  override fun isEmpty() = coordinates.isEmpty()

  fun isClosed(): Boolean =
    coordinates.size >= 4 && coordinates.firstOrNull() == coordinates.lastOrNull()

  companion object {
    fun lineStringOf(vararg coordinates: Coordinates) = LineString(coordinates.asList())
  }
}

/**
 * A closed linear ring is a sequence of [Coordinates] where the first and last coordinates are
 * equal.
 */
@Serializable
@SerialName("linear_ring")
data class LinearRing(val coordinates: List<Coordinates>) : Geometry {
  override val area: Double
    get() = 0.0

  init {
    validate()
  }

  override fun center(): Coordinates = coordinates.centerOrError()

  override fun isEmpty() = coordinates.isEmpty()

  override fun validate() {
    // TODO: Check for vertices count > 3
    // Issue URL: https://github.com/google/ground-android/issues/1647
    if (coordinates.isEmpty()) {
      return
    }
    if (coordinates.firstOrNull() != coordinates.lastOrNull()) {
      throw InvalidGeometryException("Invalid linear ring")
    }
  }

  /**
   * Returns *synthetic* coordinates containing the maximum `x` and `y` coordinates of this ring.
   */
  private fun maximum(): Coordinates {
    val maximumLat = this.coordinates.maxOfOrNull { it.lat }
    val maximumLng = this.coordinates.maxOfOrNull { it.lng }

    return Coordinates(maximumLat ?: 0.0, maximumLng ?: 0.0)
  }

  /**
   * Returns *synthetic* coordinates containing the minimum `x` and `y` coordinates of this ring.
   */
  private fun minimum(): Coordinates {
    val minimumLat = this.coordinates.minOfOrNull { it.lat }
    val minimumLng = this.coordinates.minOfOrNull { it.lng }

    return Coordinates(minimumLat ?: 0.0, minimumLng ?: 0.0)
  }

  /**
   * Returns true if this linear ring contains another.
   *
   * This is based on enveloping each ring and is equivalent to JTS's Envelope.covers method.
   */
  fun contains(other: LinearRing) =
    this.maximum() >= other.maximum() && this.minimum() <= other.minimum()
}

/**
 * Returns the center coordinates of the bounding box from the given list of coordinates.
 *
 * Note: This might return an unexpected result for oddly shaped polygons. Check if this can be
 * replaced with a centroid. See (#1737) for more info.
 */
private fun List<Coordinates>?.centerOrError(): Coordinates =
  this?.map { Point(it) }?.toBounds()?.center() ?: error("missing vertices")
