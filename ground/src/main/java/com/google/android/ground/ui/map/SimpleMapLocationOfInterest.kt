package com.google.android.ground.ui.map

import com.google.android.ground.model.locationofinterest.LocationOfInterest

/**
 * Simple wrapper around LocationOfInterest, implementing MapLocationOfInterest. Can be cleaned up
 * when the MapLocationOfInterest is removed.
 */
data class SimpleMapLocationOfInterest(
  private val locationOfInterest: LocationOfInterest
) : MapLocationOfInterest() {
  override fun getLocationOfInterest(): LocationOfInterest = locationOfInterest
}