package com.google.android.ground.ui.map.gms

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.map.Bounds

fun LatLng.toModelObject(): Coordinate = Coordinate(this.latitude, this.longitude)

fun Coordinate.toGoogleMapsObject(): LatLng = LatLng(this.x, this.y)

fun LatLngBounds.toModelObject(): Bounds =
  Bounds(this.southwest.toModelObject(), this.northeast.toModelObject())

fun Bounds.toGoogleMapsObject(): LatLngBounds =
  LatLngBounds(this.southwest.toGoogleMapsObject(), this.northeast.toGoogleMapsObject())
