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
import androidx.annotation.ColorInt
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.ui.IconFactory
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.MARKER_Z
import com.google.android.ground.ui.map.gms.toLatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PointFeatureManager @Inject constructor(@ApplicationContext val context: Context) :
  FeatureManager() {
  private val markerIconFactory: IconFactory = IconFactory(context)
  private val markersByTag = HashMap<Feature.Tag, Marker>()

  override fun addFeature(feature: Feature, isSelected: Boolean) {
    if (feature.geometry !is Point)
      error("Invalid geometry type ${feature.geometry.javaClass.simpleName}")
    val markerOptions = MarkerOptions()
    markerOptions.position(feature.geometry.coordinates.toLatLng())
    setMarkerOptions(markerOptions, isSelected, feature.style.color)
    val marker = map.addMarker(markerOptions) ?: error("Failed to create marker")
    marker.tag = feature.tag
    markersByTag[feature.tag] = marker
  }

  fun setMarkerOptions(markerOptions: MarkerOptions, isSelected: Boolean, @ColorInt color: Int) {
    with(markerOptions) {
      icon(getMarkerIcon(isSelected, color))
      zIndex(MARKER_Z)
    }
  }

  fun getMarkerIcon(isSelected: Boolean = false, @ColorInt color: Int): BitmapDescriptor =
    markerIconFactory.getMarkerIcon(color, map.cameraPosition.zoom, isSelected)

  override fun removeStaleFeatures(features: Set<Feature>) =
    (markersByTag.keys - features.map { it.tag }.toSet()).forEach { remove(it) }

  private fun remove(tag: Feature.Tag) {
    markersByTag[tag]?.remove()
    markersByTag.remove(tag)
  }

  override fun removeAllFeatures() = markersByTag.keys.forEach { remove(it) }
}
