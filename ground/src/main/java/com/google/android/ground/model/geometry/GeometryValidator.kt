/*
 * Copyright 2023 Google LLC
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

object GeometryValidator {
  private fun List<Any>.isFirstAndLastSame(): Boolean = firstOrNull() == lastOrNull()

  /** Validates that the current [LinearRing] is well-formed. */
  fun LinearRing.validate() {
    if (coordinates.isEmpty()) {
      return
    }
    if (!coordinates.isFirstAndLastSame()) {
      error("Invalid linear ring")
    }
  }

  /** Returns true if the current geometry is closed. */
  fun Geometry?.isClosedGeometry(): Boolean = this is Polygon || this is LinearRing

  /** Returns true of the current list of vertices can generate a polygon. */
  fun List<Coordinate>.isComplete(): Boolean = size >= 4 && isFirstAndLastSame()
}
