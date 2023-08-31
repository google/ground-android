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
package com.google.android.ground.ui.map.gms

import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.ui.map.Bounds

/** Extensions for indirectly using GMS functions in map-provider agnostic codebase. */
object GmsExt {

  fun Bounds.contains(coordinates: Coordinates): Boolean =
    toGoogleMapsObject().contains(coordinates.toGoogleMapsObject())

  fun Bounds.contains(geometry: Geometry): Boolean {
    val latLngBounds = toGoogleMapsObject()
    return geometry.toLatLngList().any { latLngBounds.contains(it) }
  }

  fun Bounds.center(): Coordinates = toGoogleMapsObject().center.toModelObject()

  fun List<Geometry>.toBounds(): Bounds? {
    val coordinates = this.map { it.vertices.first().coordinates }
    if (coordinates.isNotEmpty()) {
      val bounds = LatLngBounds.builder()
      coordinates.forEach { bounds.include(it.toGoogleMapsObject()) }
      return bounds.build().toModelObject()
    }

    return null
  }
}
