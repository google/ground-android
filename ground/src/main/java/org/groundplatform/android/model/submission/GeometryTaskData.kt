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
package org.groundplatform.android.model.submission

import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon

/** A user-provided response to a geometry-based task ("drop a pin" or "draw an area"). */
abstract class GeometryTaskData(val geometry: Geometry) : TaskData {

  // TODO(#1733): Move strings to view layer.
  override fun getDetailsText(): String =
    when (geometry) {
      is Point -> "Point data"
      is Polygon -> "Polygon data"
      is LinearRing -> "LinearRing data"
      is LineString -> "LineString data"
      is MultiPolygon -> "MultiPolygon data"
    }

  override fun isEmpty(): Boolean = false
}
