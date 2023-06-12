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

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CustomCap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.toLatLng

class PolylineRenderer(
  map: GoogleMap,
  private val customCap: CustomCap,
  private val strokeWidth: Float,
  private val strokeColor: Int
) : FeatureRenderer(map) {

  private val polylines: MutableMap<Feature, MutableList<Polyline>> = HashMap()

  override fun addFeature(feature: Feature, geometry: Geometry) {
    val points = geometry.vertices
    val options = PolylineOptions()
    options.clickable(false)

    val shellVertices = points.map { it.toLatLng() }
    options.addAll(shellVertices)

    val polyline: Polyline = map.addPolyline(options)
    polyline.tag = points
    polyline.startCap = customCap
    polyline.endCap = customCap
    polyline.width = strokeWidth
    polyline.color = strokeColor
    polyline.jointType = JointType.ROUND

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
