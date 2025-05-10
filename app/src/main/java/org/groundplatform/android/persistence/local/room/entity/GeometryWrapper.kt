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
package org.groundplatform.android.persistence.local.room.entity

import org.groundplatform.android.model.geometry.Geometry
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.model.geometry.MultiPolygon
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon

data class GeometryWrapper(
  /** Non-null iff this geometry is a point. */
  val point: Point? = null,
  /** Non-null iff this geometry is a polygon. */
  val polygon: Polygon? = null,
  /** Non-null iff this geometry is a multi-polygon. */
  val multiPolygon: MultiPolygon? = null,
  /** Non-null iff this geometry is a line-string. */
  val linestring: LineString? = null,
) {

  fun getGeometry(): Geometry = point ?: polygon ?: multiPolygon ?: linestring!!

  companion object {
    fun fromGeometry(geometry: Geometry?): GeometryWrapper =
      when (geometry) {
        is Point -> GeometryWrapper(point = geometry)
        is Polygon -> GeometryWrapper(polygon = geometry)
        is MultiPolygon -> GeometryWrapper(multiPolygon = geometry)
        is LineString -> GeometryWrapper(linestring = geometry)
        else -> error("No matching geometry found")
      }
  }
}
