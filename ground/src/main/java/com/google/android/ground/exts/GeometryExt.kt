package com.google.android.ground.exts

import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.gms.toGoogleMapsObject
import com.google.android.ground.ui.map.gms.toLatLng

/** Extensions for indirectly using GMS functions in map-provider agnostic codebase. */
object GeometryExt {

  fun Bounds.contains(geometry: Geometry): Boolean {
    return geometry.vertices.any { toGoogleMapsObject().contains(it.toLatLng()) }
  }
}
