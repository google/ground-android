/*
 * Copyright 2024 Google LLC
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

package com.google.android.ground.ui.map.gms.features

import android.content.res.Resources
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.ground.R
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.ui.IconFactory
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.MARKER_Z
import com.google.android.ground.ui.map.gms.toLatLng
import javax.inject.Inject

class PointRenderer
@Inject
constructor(resources: Resources, private val markerIconFactory: IconFactory) :
  MapItemRenderer<Point, Marker> {

  private val defaultMarkerScale =
    ResourcesCompat.getFloat(resources, R.dimen.marker_bitmap_default_scale)
  private val selectedMarkerScaleFactor =
    ResourcesCompat.getFloat(resources, R.dimen.marker_bitmap_selected_scale_factor)

  override fun addMapItem(
    map: GoogleMap,
    tag: Feature.Tag,
    geometry: Point,
    style: Feature.Style
  ): Marker {
    val markerOptions = MarkerOptions()
    markerOptions.position(geometry.coordinates.toLatLng())
    setMarkerOptions(markerOptions, style)
    val marker = map.addMarker(markerOptions) ?: error("Failed to create marker")
    marker.tag = tag
    return marker
  }

  private fun setMarkerOptions(markerOptions: MarkerOptions, style: Feature.Style) {
    with(markerOptions) {
      icon(getMarkerIcon(style))
      zIndex(MARKER_Z)
    }
  }

  private fun getMarkerIcon(style: Feature.Style): BitmapDescriptor {
    // TODO(!!!): How should we update scale based on zoom level? Current impl is likely broken.
    var scale = defaultMarkerScale
    if (style.selected) {
      scale *= selectedMarkerScaleFactor
    }
    return markerIconFactory.getMarkerIcon(style.color, scale)
  }

  override fun remove(mapItem: Marker) {
    mapItem.remove()
  }
}
