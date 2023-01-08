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

import com.google.common.collect.ImmutableList

/**
 * A polygon made up of a linear ring that dictates its bounds and any number of holes within the
 * shell ring.
 */
data class Polygon(
  val shell: LinearRing,
  val holes: ImmutableList<LinearRing> = ImmutableList.of()
) : Geometry {
  /**
   * Constructs a [Polygon] using the specified shell and an immutable copy of the specified mutable
   * list of holes.
   */
  constructor(shell: LinearRing, holes: List<LinearRing>) : this(shell, ImmutableList.copyOf(holes))

  override val vertices: ImmutableList<Point> = shell.vertices
}
