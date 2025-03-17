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
package org.groundplatform.android.ui.map.gms

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.ui.map.Bounds

fun LatLng.toModelObject(): Coordinates = Coordinates(this.latitude, this.longitude)

fun Coordinates.toGoogleMapsObject(): LatLng = LatLng(this.lat, this.lng)

fun LatLngBounds.toModelObject(): Bounds =
  Bounds(this.southwest.toModelObject(), this.northeast.toModelObject())

fun Bounds.toGoogleMapsObject(): LatLngBounds =
  LatLngBounds(this.southwest.toGoogleMapsObject(), this.northeast.toGoogleMapsObject())

fun LatLng.toCoordinates(): Coordinates = Coordinates(latitude, longitude)

fun Coordinates.toLatLng(): LatLng = LatLng(lat, lng)

fun List<Coordinates>.toLatLngList(): List<LatLng> = map { it.toLatLng() }

fun Location.toCoordinates(): Coordinates = Coordinates(latitude, longitude)

fun Location.getAltitudeOrNull(): Double? = if (hasAltitude()) altitude else null

fun Location.getAccuracyOrNull(): Double? = if (hasAccuracy()) accuracy.toDouble() else null
