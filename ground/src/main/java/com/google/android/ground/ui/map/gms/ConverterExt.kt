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
package com.google.android.ground.ui.map.gms

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.ui.map.Bounds

fun LatLng.toModelObject(): Coordinates = Coordinates(this.latitude, this.longitude)

fun Coordinates.toGoogleMapsObject(): LatLng = LatLng(this.lat, this.lng)

fun LatLngBounds.toModelObject(): Bounds =
  Bounds(this.southwest.toModelObject(), this.northeast.toModelObject())

fun Bounds.toGoogleMapsObject(): LatLngBounds =
  LatLngBounds(this.southwest.toGoogleMapsObject(), this.northeast.toGoogleMapsObject())

fun LatLng.toCoordinates(): Coordinates = Coordinates(latitude, longitude)

fun Point.toLatLng(): LatLng = LatLng(coordinates.lat, coordinates.lng)

fun Coordinates.toLatLng(): LatLng = LatLng(lat, lng)

fun Location.toCoordinates(): Coordinates = Coordinates(latitude, longitude)

fun Geometry.toCoordinates(): List<Coordinates> =
  when (this) {
    is Point -> listOf(coordinates)
    is LineString -> coordinates
    is LinearRing -> coordinates
    is Polygon -> shell.coordinates
    is MultiPolygon -> polygons.flatMap { it.shell.coordinates }
  }

fun Geometry.toLatLngList(): List<LatLng> = toCoordinates().map { it.toLatLng() }
