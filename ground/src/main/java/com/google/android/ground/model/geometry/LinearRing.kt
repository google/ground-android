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

import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList

/**
 * A closed linear ring is a sequence of [Coordinate]s where the first and last coordinates are
 * equal.
 */
data class LinearRing(val coordinates: ImmutableList<Coordinate>) : Geometry {
  /** Constructs a [LinearRing] using an immutable copy of the provided mutable list. */
  constructor(coordinates: List<Coordinate>) : this(ImmutableList.copyOf(coordinates))

  override val vertices: ImmutableList<Point> = coordinates.map { Point(it) }.toImmutableList()

  /**
   * Returns a *synthetic* coordinate containing the maximum x and y coordinate values of this ring.
   */
  fun maximum(): Coordinate {
    val maximumX = this.coordinates.map { it.x }.maxOrNull()
    val maximumY = this.coordinates.map { it.y }.maxOrNull()

    return Coordinate(maximumX ?: 0.0, maximumY ?: 0.0)
  }

  /**
   * Returns a *synthetic* coordinate containing the minimum x and y coordinate values of this ring.
   */
  private fun minimum(): Coordinate {
    val minimumX = this.coordinates.map { it.x }.minOrNull()
    val minimumY = this.coordinates.map { it.y }.minOrNull()

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
