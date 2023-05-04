package com.google.android.ground.exts

import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.gms.toGoogleMapsObject
import com.google.android.ground.ui.map.gms.toLatLng
import com.google.android.ground.ui.map.gms.toModelObject

/** Extensions for indirectly using GMS functions in map-provider agnostic codebase. */
object GeometryExt {

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
}
