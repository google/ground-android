package com.google.android.ground.ui.map.gms

import com.google.android.gms.maps.GoogleMap
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.ui.map.Bounds

/** Extensions for indirectly using GMS functions in map-provider agnostic codebase. */
object GmsExt {

  fun Bounds.contains(coordinate: Coordinate): Boolean {
    return toGoogleMapsObject().contains(coordinate.toGoogleMapsObject())
  }

  fun Bounds.contains(geometry: Geometry): Boolean {
    val latLngBounds = toGoogleMapsObject()
    return geometry.vertices.any { latLngBounds.contains(it.toLatLng()) }
  }

  fun Bounds.center(): Coordinate {
    return toGoogleMapsObject().center.toModelObject()
  }

  fun defaultMapType(): Int = GoogleMap.MAP_TYPE_HYBRID
}
