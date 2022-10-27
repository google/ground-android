package com.google.android.ground.ui.map.gms

import com.google.android.gms.maps.model.LatLng
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.maps.android.clustering.ClusterItem

class LocationOfInterestClusterItem(
  position: Point,
  title: String,
  snippet: String,
  val locationOfInterest: LocationOfInterest,
) : ClusterItem {
  private val position: Point
  private val title: String
  private val snippet: String

  override fun getPosition(): LatLng = position.coordinate.toGoogleMapsObject()

  override fun getTitle(): String = title

  override fun getSnippet(): String = snippet

  init {
    this.position = position
    this.title = title
    this.snippet = snippet
  }
}
