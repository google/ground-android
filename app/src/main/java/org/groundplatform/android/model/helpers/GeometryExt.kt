/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.model.helpers

import com.google.maps.android.SphericalUtil.computeArea
import org.groundplatform.android.ui.map.gms.toLatLngList
import org.groundplatform.domain.model.geometry.Geometry
import org.groundplatform.domain.model.geometry.LineString
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.MultiPolygon
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.geometry.Polygon

fun Geometry.area(): Double =
  when (this) {
    is Point -> 0.0
    is LineString -> 0.0
    is LinearRing -> 0.0
    is Polygon ->
      computeArea(shell.coordinates.toLatLngList()) -
        holes.sumOf { computeArea(it.coordinates.toLatLngList()) }
    is MultiPolygon -> polygons.sumOf { it.area() }
  }
