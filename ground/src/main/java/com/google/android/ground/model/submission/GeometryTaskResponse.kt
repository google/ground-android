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
package com.google.android.ground.model.submission

import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon

/** A user-provided response to a geometry-based task ("drop a pin" or "draw an area"). */
data class GeometryTaskResponse(val geometry: Geometry) : Value {

  // TODO(#1733): Figure out what should be the user visible text for geometry data.
  override fun getDetailsText(): String =
    when (geometry) {
      is Point -> "Point data"
      is Polygon -> "Polygon data"
      is LinearRing -> "LinearRing data"
      is LineString -> "LineString data"
      is MultiPolygon -> "MultiPolygon data"
    }

  override fun isEmpty(): Boolean = false

  companion object {
    fun fromGeometry(geometry: Geometry?): GeometryTaskResponse? =
      geometry?.let { GeometryTaskResponse(it) }
  }
}
