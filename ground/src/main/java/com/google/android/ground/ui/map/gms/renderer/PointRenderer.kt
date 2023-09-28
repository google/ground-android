/*
 * Copyright 2023 Google LLC
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

package com.google.android.ground.ui.map.gms.renderer

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.ui.IconFactory
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.MARKER_Z
import com.google.android.ground.ui.map.gms.parseColor
import com.google.android.ground.ui.map.gms.toLatLng

class PointRenderer(val context: Context, map: GoogleMap) : FeatureRenderer(map) {
  private val markerIconFactory: IconFactory = IconFactory(context)

  override fun addFeature(feature: Feature, isSelected: Boolean) {
    if (feature.geometry !is Point)
      error("Invalid geometry type ${feature.geometry.javaClass.simpleName}")
    val markerOptions = MarkerOptions()
    markerOptions.position(feature.geometry.coordinates.toLatLng())
    setMarkerOptions(markerOptions, isSelected, feature.style.color)
    map.addMarker(markerOptions)
  }

  fun setMarkerOptions(markerOptions: MarkerOptions, isSelected: Boolean, color: String) {
    with(markerOptions) {
      icon(getMarkerIcon(isSelected, color))
      zIndex(MARKER_Z)
    }
  }

  fun getMarkerIcon(isSelected: Boolean = false, color: String): BitmapDescriptor =
    markerIconFactory.getMarkerIcon(
      color.parseColor(context.resources),
      map.cameraPosition.zoom,
      isSelected
    )

  override fun removeStaleFeatures(features: Set<Feature>) {
    TODO("Not yet implemented")
  }

  override fun removeAllFeatures() {
    TODO("Not yet implemented")
  }
}
