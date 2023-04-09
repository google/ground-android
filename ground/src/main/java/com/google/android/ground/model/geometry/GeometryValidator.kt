package com.google.android.ground.model.geometry

class GeometryValidator {

  companion object {

    private fun List<Any>.isFirstAndLastSame(): Boolean {
      return firstOrNull() == lastOrNull()
    }

    /** Validates that the current [LinearRing] is well-formed. */
    fun LinearRing.validateLinearRing() {
      if (coordinates.isEmpty()) {
        return
      }
      if (!coordinates.isFirstAndLastSame()) {
        error("Invalid linear ring")
      }
    }

    /** Returns true if the current geometry is closed. */
    fun Geometry?.isClosedGeometry(): Boolean {
      return this is Polygon || this is LinearRing
    }

    /** Returns true of the current list of vertices can generate a polygon. */
    fun List<Coordinate>.isComplete(): Boolean {
      if (size < 4) return false
      return isFirstAndLastSame()
    }
  }
}
