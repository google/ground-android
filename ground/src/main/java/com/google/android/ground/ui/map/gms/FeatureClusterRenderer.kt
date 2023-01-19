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

import android.content.Context
import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.ground.R
import com.google.android.ground.model.job.Style
import com.google.android.ground.ui.MarkerIconFactory
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import timber.log.Timber

/** A cluster renderer for [FeatureClusterItem]s. */
class FeatureClusterRenderer(
  private val context: Context?,
  private val map: GoogleMap,
  private val clusterManager: FeatureClusterManager,
) : DefaultClusterRenderer<FeatureClusterItem>(context, map, clusterManager) {

  private val markerIconFactory: MarkerIconFactory? = context?.let { MarkerIconFactory(it) }

  private fun parseColor(colorHexCode: String?): Int =
    try {
      Color.parseColor(colorHexCode.toString())
    } catch (e: IllegalArgumentException) {
      Timber.w("Invalid color code in job style: $colorHexCode")
      context?.resources?.getColor(R.color.colorMapAccent) ?: 0
    }

  private fun getMarkerIcon(isSelected: Boolean = false): BitmapDescriptor? =
    markerIconFactory?.getMarkerIcon(parseColor(Style().color), map.cameraPosition.zoom, isSelected)

  /** Sets appropriate styling for clustered markers prior to rendering. */
  override fun onBeforeClusterItemRendered(item: FeatureClusterItem, markerOptions: MarkerOptions) {
    if (item.feature.tag.id == clusterManager.activeLocationOfInterest) {
      markerOptions.icon(getMarkerIcon(true))
    } else {
      markerOptions.icon(getMarkerIcon(false))
    }
  }

  private fun createMarker(cluster: Cluster<FeatureClusterItem>): BitmapDescriptor? {
    var totalWithData = 0

    cluster.items.forEach {
      if (it.feature.tag.flag) {
        totalWithData++
      }
    }

    val icon =
      markerIconFactory?.getClusterIcon(
        parseColor(Style().color),
        map.cameraPosition.zoom,
        "$totalWithData/" + cluster.items.size
      )
    return icon
  }

  override fun onBeforeClusterRendered(
    cluster: Cluster<FeatureClusterItem>,
    markerOptions: MarkerOptions
  ) {
    super.onBeforeClusterRendered(cluster, markerOptions)

    markerOptions.icon(createMarker(cluster))
  }

  override fun onClusterUpdated(cluster: Cluster<FeatureClusterItem>, marker: Marker) {
    super.onClusterUpdated(cluster, marker)
    marker.setIcon(createMarker(cluster))
  }
}
