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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CustomCap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.ground.R
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.toLatLngList
import com.google.android.ground.ui.util.BitmapUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

class PolylineRenderer
@Inject
constructor(@ApplicationContext context: Context, bitmapUtil: BitmapUtil) : FeatureRenderer() {
  private val polylines: MutableMap<Feature, MutableList<Polyline>> = HashMap()
  private val lineWidth = context.resources.getDimension(R.dimen.line_geometry_width)
  private val circleCap by lazy {
    // This must be done lazily since resources are not available before the app completes
    // initialization.
    val bitmap = bitmapUtil.fromVector(R.drawable.ic_endpoint)
    CustomCap(BitmapDescriptorFactory.fromBitmap(bitmap))
  }

  override fun addFeature(feature: Feature, isSelected: Boolean) {
    when (feature.geometry) {
      is LineString -> render(feature, feature.geometry.coordinates, isSelected)
      is LinearRing -> render(feature, feature.geometry.coordinates, isSelected)
      else ->
        throw IllegalArgumentException(
          "PolylineRendered expected LineString or LinearRing, but got ${feature.geometry::class.simpleName}"
        )
    }
  }

  private fun render(feature: Feature, points: List<Coordinates>, isSelected: Boolean) {
    Timber.d("Adding Polyline $feature")

    val options = PolylineOptions()
    with(options) {
      clickable(false)
      addAll(points.toLatLngList())
    }
    val polyline = map.addPolyline(options)
    val strokeScale = if (isSelected) 2f else 1f
    val style = feature.style
    with(polyline) {
      tag = feature.tag
      if (style.jointType == Feature.JointType.CIRCLE) {
        startCap = circleCap
        endCap = circleCap
      }
      width = lineWidth * strokeScale
      color = style.color
      jointType = JointType.ROUND
    }

    polylines.getOrPut(feature) { mutableListOf() }.add(polyline)
  }

  override fun removeStaleFeatures(features: Set<Feature>) {
    val deletedIds = polylines.keys.map { it.tag.id } - features.map { it.tag.id }.toSet()
    val deletedPolylines = polylines.filter { deletedIds.contains(it.key.tag.id) }
    deletedPolylines.values.forEach { it.forEach(Polyline::remove) }
    polylines.minusAssign(deletedPolylines.keys)
  }

  override fun removeAllFeatures() {
    polylines.values.forEach { it.forEach(Polyline::remove) }
    polylines.clear()
  }
}
