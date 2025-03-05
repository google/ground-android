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
package org.groundplatform.android.ui.map.gms

import com.google.android.gms.maps.model.LatLngBounds
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.Geometry
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.MultiPolygon
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.ui.map.Bounds

/** Extensions for indirectly using GMS functions in map-provider agnostic codebase. */
object GmsExt {

  fun Bounds.contains(geometry: Geometry): Boolean {
    val latLngBounds = toGoogleMapsObject()
    return geometry.getShellCoordinates().any { latLngBounds.contains(it.toLatLng()) }
  }

  fun Bounds.center(): Coordinates = toGoogleMapsObject().center.toModelObject()

  fun List<Geometry>.toBounds(): Bounds? {
    // TODO: Don't use shell coordinates for polygon and multi-polygons.
    // Issue URL: https://github.com/google/ground-android/issues/1825
    val coordinates = this.flatMap { it.getShellCoordinates() }
    if (coordinates.isNotEmpty()) {
      val bounds = LatLngBounds.builder()
      coordinates.forEach { bounds.include(it.toGoogleMapsObject()) }
      return bounds.build().toModelObject()
    }

    return null
  }

  /** Returns the list of [Coordinates] in the geometry or in the outer shell of the geometry. */
  fun Geometry.getShellCoordinates(): List<Coordinates> =
    when (this) {
      is Point -> listOf(coordinates)
      is LineString -> coordinates
      is LinearRing -> coordinates
      is Polygon -> getShellCoordinates()
      is MultiPolygon -> polygons.flatMap { it.getShellCoordinates() }
    }
}
