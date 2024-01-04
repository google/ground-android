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

import com.google.android.gms.maps.GoogleMap
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.ui.map.Feature
import javax.inject.Inject

class FeatureManager
@Inject
constructor(
  pointRenderer: PointRenderer,
  polygonRenderer: PolygonRenderer,
  multiPolygonRenderer: MultiPolygonRenderer,
  lineStringRenderer: LineStringRenderer
) {
  private val features = mutableSetOf<Feature>()
  private val pointManager = MapItemManager(pointRenderer)
  private val polygonManager = MapItemManager(polygonRenderer)
  private val multiPolygonManager = MapItemManager(multiPolygonRenderer)
  private val lineStringManager = MapItemManager(lineStringRenderer)

  fun setFeatures(map: GoogleMap, updatedFeatures: Set<Feature>) {
    // remove stale
    val removedOrChanged = features - updatedFeatures
    removedOrChanged.forEach(this::removeMapItem)
    features.removeAll(removedOrChanged)
    // add missing
    val newOrChanged = updatedFeatures - features
    newOrChanged.forEach { setFeature(map, it) }
    features.addAll(newOrChanged)
  }

  private fun setFeature(map: GoogleMap, feature: Feature) {
    with(feature) {
      when (geometry) {
        is Point -> pointManager.set(map, tag, geometry, style)
        is LineString -> lineStringManager.set(map, tag, geometry, style)
        is LinearRing -> error("LinearRing rendering not supported")
        is MultiPolygon -> multiPolygonManager.set(map, tag, geometry, style)
        is Polygon -> polygonManager.set(map, tag, geometry, style)
      }
    }
  }

  private fun removeMapItem(feature: Feature) {
    with(feature) {
      // Remove from all managers in case geometry type changed.
      pointManager.remove(tag)
      lineStringManager.remove(tag)
      multiPolygonManager.remove(tag)
      polygonManager.remove(tag)
    }
  }
}
