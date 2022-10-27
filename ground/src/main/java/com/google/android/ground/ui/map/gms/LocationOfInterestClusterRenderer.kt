package com.google.android.ground.ui.map.gms

import android.content.Context
import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Marker
import com.google.android.ground.R
import com.google.android.ground.model.job.Style
import com.google.android.ground.ui.MarkerIconFactory
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import javax.inject.Inject
import timber.log.Timber

class LocationOfInterestClusterRenderer(
  private val context: Context?,
  private val map: GoogleMap,
  private val clusterManager: LocationOfInterestClusterManager,
) : DefaultClusterRenderer<LocationOfInterestClusterItem>(context, map, clusterManager) {

  @Inject lateinit var markerIconFactory: MarkerIconFactory

  private fun parseColor(colorHexCode: String?): Int =
    try {
      Color.parseColor(colorHexCode.toString())
    } catch (e: IllegalArgumentException) {
      Timber.w("Invalid color code in job style: $colorHexCode")
      context?.resources?.getColor(R.color.colorMapAccent) ?: 0
    }

  private fun getMarkerIcon(isSelected: Boolean = false): BitmapDescriptor =
    markerIconFactory.getMarkerIcon(parseColor(Style().color), map.cameraPosition.zoom, isSelected)

  override fun onClusterItemRendered(clusterItem: LocationOfInterestClusterItem, marker: Marker) {
    super.onClusterItemRendered(clusterItem, marker)

    if (clusterItem.locationOfInterest.id == clusterManager.activeLocationOfInterest) {
      marker.setIcon(getMarkerIcon(true))
    } else {
      marker.setIcon(getMarkerIcon(false))
    }
  }
}
