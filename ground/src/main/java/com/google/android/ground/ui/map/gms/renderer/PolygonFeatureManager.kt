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
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.Polygon as MapsPolygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.ground.R
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.POLYGON_Z
import com.google.android.ground.ui.map.gms.toLatLng
import com.google.android.ground.ui.map.gms.toLatLngList
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

class PolygonFeatureManager @Inject constructor(@ApplicationContext context: Context) :
  FeatureManager() {
  private val polygonsByFeature: MutableMap<Feature, MutableList<MapsPolygon>> = HashMap()
  private val features: Set<Feature>
    get() = polygonsByFeature.keys

  private val lineWidth = context.resources.getDimension(R.dimen.line_geometry_width)

  override fun setFeatures(newFeatures: Set<Feature>) {
    val newPolygonFeatures = newFeatures.filter { it.geometry is Polygon }.toSet()
    val staleFeatures = features - newPolygonFeatures
    removeFeatures(staleFeatures)
    val missingFeatures = newPolygonFeatures - features
    missingFeatures.forEach(this::putFeature)
  }

  private fun putFeature(feature: Feature) {
    val style = feature.style
    when (feature.geometry) {
      is Polygon -> putFeature(feature, feature.geometry)
      is MultiPolygon -> feature.geometry.polygons.map { putFeature(feature, it) }
      else ->
        throw IllegalArgumentException(
          "PolylineRendered expected Polygon or MultiPolygon, but got ${feature.geometry::class.simpleName}"
        )
    }
  }

  private fun putFeature(feature: Feature, polygon: Polygon) {
    Timber.v("Adding polygon $feature")

    val options = PolygonOptions()
    with(options) {
      clickable(false)
      addAll(polygon.getShellCoordinates().map { it.toLatLng() })
    }

    polygon.holes.forEach { options.addHole(it.coordinates.toLatLngList()) }

    val mapsPolygon = map.addPolygon(options)
    val strokeScale = if (feature.style.selected) 2f else 1f
    with(mapsPolygon) {
      tag = Pair(feature.tag.id, LocationOfInterest::javaClass)
      strokeWidth = lineWidth * strokeScale
      strokeColor = feature.style.color
      strokeJointType = JointType.ROUND
      zIndex = POLYGON_Z
    }
    polygonsByFeature.getOrPut(feature) { mutableListOf() }.add(mapsPolygon)
  }

  fun getPolygonsByFeature(): Map<Feature, MutableList<MapsPolygon>> = polygonsByFeature

  private fun removeFeatures(features: Set<Feature>) = features.forEach(this::removeFeature)

  private fun removeFeature(feature: Feature) =
    polygonsByFeature.remove(feature)?.let { polygons -> polygons.forEach { it.remove() } }

  fun updateFeature(feature: Feature) {
    removeFeature(feature)
    putFeature(feature)
  }
}
